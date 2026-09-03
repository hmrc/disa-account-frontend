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

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.PageController
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.LiaisonOfficerEmailFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficers.findLiaisonOfficer
import uk.gov.hmrc.disaaccountfrontend.models.pages.LiaisonOfficerEmailPage
import uk.gov.hmrc.disaaccountfrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.liaisonofficers.LiaisonOfficerEmailView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LiaisonOfficerEmailController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: LiaisonOfficerEmailFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: LiaisonOfficerEmailView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport {

  private val form: Form[String] = formProvider()

  private def page(id: String): LiaisonOfficerEmailPage =
    LiaisonOfficerEmailPage(id)

  private def pageAction(currentPage: LiaisonOfficerEmailPage) =
    identify andThen getData andThen guardPage(currentPage)

  def onPageLoad(id: String, mode: Mode): Action[AnyContent] = {
    val currentPage = page(id)

    pageAction(currentPage) { implicit request =>
      val liaisonOfficer = findLiaisonOfficer(id)
      val preparedForm   = liaisonOfficer.flatMap(_.email).fold(form)(form.fill)

      Ok(view(id, liaisonOfficer.flatMap(_.fullName).getOrElse(""), mode, preparedForm))
    }
  }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = {
    val currentPage = page(id)

    pageAction(currentPage).async { implicit request =>
      val name = findLiaisonOfficer(id).flatMap(_.fullName).getOrElse("")

      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, name, mode, formWithErrors))),
          answer => {
            val sessionUpdates = getSessionUpdates(currentPage, answer)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
              .map(_ => Redirect(nextPage(currentPage, sessionUpdates, mode)))
          }
        )
    }
  }
}
