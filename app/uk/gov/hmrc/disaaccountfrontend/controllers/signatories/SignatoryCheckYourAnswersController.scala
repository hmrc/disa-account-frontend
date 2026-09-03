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

package uk.gov.hmrc.disaaccountfrontend.controllers.signatories

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryCheckYourAnswersPage
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.signatories.{SignatoryJobTitleSummary, SignatoryNameSummary}
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.SignatoryCheckYourAnswersView
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject

class SignatoryCheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryCheckYourAnswersView
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] =
    (identify andThen getData andThen guardPage(SignatoryCheckYourAnswersPage(id))) { implicit request =>
      request.effectiveAnswers.signatories
        .flatMap(_.signatories.find(_.id == id))
        .fold(Redirect(ChangeOfCircumstancesController.onPageLoad())) { signatory =>
          val rows = Seq(SignatoryNameSummary.row(signatory), SignatoryJobTitleSummary.row(signatory)).flatten
          Ok(view(SummaryList(rows = rows)))
        }
    }
}
