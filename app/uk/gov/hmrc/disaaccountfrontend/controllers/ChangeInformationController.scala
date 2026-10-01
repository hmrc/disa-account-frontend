/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.disaaccountfrontend.controllers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{AccountMaintenanceGuardAction, DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.ChangeInformationFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{AuthorisedUsers, IsaProductInformation, OrganisationInformation, ViewAllInformation, viewAllInformationFormValue}
import uk.gov.hmrc.disaaccountfrontend.models.{ChangeInformationSelection, SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.ChangeInformation
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeInformationController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  accountMaintenanceGuard: AccountMaintenanceGuardAction,
  userAnswersRepository: UserAnswersRepository,
  formProvider: ChangeInformationFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeInformation
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val pageAction = identify andThen getData andThen accountMaintenanceGuard

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    val availableSelections = ChangeInformationSelection.availableValues(request.isSignatory)
    val form                = formProvider(availableSelections)
    val preparedForm        = request.sessionAnswers
      .map(_.updates.changeInformationSelections)
      .collect { case Assign(selections) =>
        val formValues =
          if (selections.contains(ViewAllInformation)) Set(viewAllInformationFormValue)
          else selections.filter(availableSelections.contains).map(_.toString).toSet

        form.fill(formValues)
      }
      .getOrElse(form)

    Ok(view(preparedForm, availableSelections))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    val availableSelections = ChangeInformationSelection.availableValues(request.isSignatory)
    val form                = formProvider(availableSelections)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, availableSelections))),
        formValues => {
          val selections       = ChangeInformationSelection.fromForm(formValues, availableSelections)
          val existingUpdates  = request.sessionAnswers.fold(SessionUpdates())(_.updates)
          val previousSelected = existingUpdates.changeInformationSelections match {
            case Assign(previous) => Some(previous)
            case _                => None
          }
          val updatedUpdates   = previousSelected
            .fold(existingUpdates)(previous => clearHiddenSections(existingUpdates, previous, selections))
            .copy(changeInformationSelections = Assign(selections))
          val updatedAnswers   = UserAnswers(id = request.sessionId, updates = updatedUpdates)

          userAnswersRepository
            .set(updatedAnswers)
            .map(_ => Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
        }
      )
  }

  private def clearHiddenSections(
    updates: SessionUpdates,
    previousSelections: Seq[ChangeInformationSelection],
    newSelections: Seq[ChangeInformationSelection]
  ): SessionUpdates =
    hiddenFieldHousekeeping.foldLeft(updates) { case (acc, (section, clearFields)) =>
      val wasShown = ChangeInformationSelection.isShown(previousSelections, section)
      val isShown  = ChangeInformationSelection.isShown(newSelections, section)
      if (wasShown && !isShown) clearFields(acc) else acc
    }

  private val hiddenFieldHousekeeping: Seq[(ChangeInformationSelection, SessionUpdates => SessionUpdates)] = Seq(
    OrganisationInformation -> ((updates: SessionUpdates) =>
      updates.copy(
        correspondenceAddress = Unchanged,
        organisationTelephoneNumber = Unchanged,
        tradingName = Unchanged,
        organisationEmailAddress = Unchanged,
        organisationEmailVerified = Unchanged
      )
    ),
    IsaProductInformation   -> ((updates: SessionUpdates) =>
      updates.copy(
        isaProducts = Unchanged,
        innovativeFinancialProducts = Unchanged,
        p2pPlatform = Unchanged,
        p2pPlatformNumber = Unchanged,
        fcaArticles = Unchanged,
        financialOrganisation = Unchanged
      )
    ),
    AuthorisedUsers         -> ((updates: SessionUpdates) => updates.copy(signatories = Unchanged, liaisonOfficers = Unchanged))
  )
}
