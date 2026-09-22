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
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{AuthorisedUsers, IsaProductInformation, OrganisationInformation}
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances.*
import uk.gov.hmrc.disaaccountfrontend.views.html.ChangeOfCircumstancesView
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class ChangeOfCircumstancesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeOfCircumstancesView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val answers    = request.effectiveAnswers
    val selections = selectedSections

    def shown(section: ChangeInformationSelection): Boolean =
      ChangeInformationSelection.isShown(selections, section)

    val organisation = Option.when(shown(OrganisationInformation))(
      summaryList(
        TradingNameSummary.row(answers),
        CorrespondenceAddressSummary.row(answers),
        OrganisationTelephoneNumberSummary.row(answers),
        OrganisationEmailSummary.row(answers)
      )
    )

    val products = Option.when(shown(IsaProductInformation) && request.isSignatory)(
      summaryList(IsaProductsSummary.row(answers), FcaArticlesSummary.row(answers))
    )

    val authorisedUsers = Option.when(shown(AuthorisedUsers))(
      summaryList(LiaisonOfficersSummary.row(answers), SignatoriesSummary.row(answers))
    )

    val productChanges        = ProductChanges(request.originalAnswers, answers)
    val signatoryChanges      = SignatoryChanges(request.originalAnswers, answers)
    val liaisonOfficerChanges = LiaisonOfficerChanges(request.originalAnswers, answers)
    val checkYourChangesRows  =
      FieldChanges.rows(request.originalAnswers, answers) ++
        productChanges.rows ++ signatoryChanges.rows ++ liaisonOfficerChanges.rows

    Ok(
      view(
        organisation.filter(_.rows.nonEmpty),
        products.filter(_.rows.nonEmpty),
        authorisedUsers.filter(_.rows.nonEmpty),
        Option.when(checkYourChangesRows.nonEmpty)(SummaryList(rows = checkYourChangesRows)),
        productChanges.hasChanges
      )
    )
  }

  private def selectedSections(implicit request: DataRequest[_]): Seq[ChangeInformationSelection] =
    request.sessionAnswers
      .map(_.updates.changeInformationSelections)
      .collect { case Assign(selections) => selections }
      .getOrElse(Seq.empty)

  private def summaryList(rows: Option[SummaryListRow]*): SummaryList =
    SummaryList(rows = rows.flatten)
}
