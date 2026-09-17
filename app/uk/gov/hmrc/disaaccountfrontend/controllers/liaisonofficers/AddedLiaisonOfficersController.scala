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

package uk.gov.hmrc.disaaccountfrontend.controllers.liaisonofficers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.liaisonofficers.AddedLiaisonOfficersSummary
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.models.pages.liaisonofficers.AddedLiaisonOfficerPage
import uk.gov.hmrc.disaaccountfrontend.views.html.liaisonofficers.AddedLiaisonOfficersView

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

class AddedLiaisonOfficersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  formProvider: YesNoAnswerFormProvider,
  navigator: Navigator,
  appConfig: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: AddedLiaisonOfficersView
) extends FrontendBaseController
    with I18nSupport {

  private val form       = formProvider("addedLiaisonOfficer.error.required")
  private val pageAction = identify andThen getData andThen guardPage(AddedLiaisonOfficerPage)

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    Ok(view(form, summary))
  }

  def onSubmit(): Action[AnyContent] = pageAction { implicit request =>
    val pageSummary = summary

    if (!pageSummary.canAddMore) {
      Redirect(ChangeOfCircumstancesController.onPageLoad())
    } else {
      form
        .bindFromRequest()
        .fold(
          formWithErrors => BadRequest(view(formWithErrors, pageSummary)),
          answer => Redirect(navigator.nextPageFromAddedLiaisonOfficer(answer))
        )
    }
  }

  private def summary(implicit request: DataRequest[_]): AddedLiaisonOfficersSummary =
    AddedLiaisonOfficersSummary(
      liaisonOfficers = request.effectiveAnswers.liaisonOfficers.fold(Seq.empty)(_.liaisonOfficers),
      maxOfficers = appConfig.maxLiaisonOfficers
    )
}
