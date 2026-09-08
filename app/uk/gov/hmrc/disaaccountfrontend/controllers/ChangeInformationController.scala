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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.ChangeInformationFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{ViewAllInformation, viewAllInformationFormValue}
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
  userAnswersRepository: UserAnswersRepository,
  formProvider: ChangeInformationFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeInformation
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val availableSelections = ChangeInformationSelection.availableValues(request.isSignatory)
    val form                = formProvider(availableSelections)
    val preparedForm = request.sessionAnswers
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

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    val availableSelections = ChangeInformationSelection.availableValues(request.isSignatory)
    val form                = formProvider(availableSelections)

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, availableSelections))),
        formValues => {
          val selections      = ChangeInformationSelection.fromForm(formValues, availableSelections)
          val existingUpdates = request.sessionAnswers.fold(SessionUpdates())(_.updates)
          val updatedAnswers = UserAnswers(
            id = request.sessionId,
            updates = existingUpdates.copy(changeInformationSelections = Assign(selections))
          )

          userAnswersRepository
            .set(updatedAnswers)
            .map(_ => Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
        }
      )
  }
}
