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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.models.pages.liaisonofficers.LiaisonOfficerCheckYourAnswersPage
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.liaisonofficers.{LiaisonOfficerEmailSummary, LiaisonOfficerNameSummary, LiaisonOfficerPhoneSummary}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.disaaccountfrontend.views.html.liaisonofficers.LiaisonOfficerCheckYourAnswersView

import javax.inject.Inject

class LiaisonOfficerCheckYourAnswersController @Inject() (
                                                           override val messagesApi: MessagesApi,
                                                           identify: IdentifierAction,
                                                           getData: DataRetrievalAction,
                                                           guardPage: PageGuardAction,
                                                           val controllerComponents: MessagesControllerComponents,
                                                           view: LiaisonOfficerCheckYourAnswersView
                                                         ) extends FrontendBaseController
  with I18nSupport {

  def onPageLoad(id: String): Action[AnyContent] =
    (identify andThen getData andThen guardPage(LiaisonOfficerCheckYourAnswersPage(id))) { implicit request =>
      request.effectiveAnswers.liaisonOfficers
        .flatMap(_.liaisonOfficers.find(_.id == id))
        .fold(Redirect(ChangeOfCircumstancesController.onPageLoad())) { officer =>
          val rows = Seq(LiaisonOfficerNameSummary.row(officer), LiaisonOfficerPhoneSummary.row(officer),LiaisonOfficerEmailSummary.row(officer)).flatten
          Ok(view(SummaryList(rows = rows)))
        }
    }

}
