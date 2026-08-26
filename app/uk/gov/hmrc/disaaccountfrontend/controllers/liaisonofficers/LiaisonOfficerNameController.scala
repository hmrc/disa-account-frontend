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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.LiaisonOfficerNameFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.LiaisonOfficerNamePage
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.UuidGenerator
import uk.gov.hmrc.disaaccountfrontend.views.html.liaisonofficers.LiaisonOfficerNameView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LiaisonOfficerNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  uuidGenerator: UuidGenerator,
  formProvider: LiaisonOfficerNameFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: LiaisonOfficerNameView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String]): Action[AnyContent] = (identify andThen getData) { implicit request =>
    id match {
      case None             =>
        Redirect(routes.LiaisonOfficerNameController.onPageLoad(Some(uuidGenerator.generate())))
      case Some(existingId) =>
        val savedName =
          request.effectiveAnswers.liaisonOfficers
            .flatMap(_.liaisonOfficers.find(_.id == existingId))
            .flatMap(_.fullName)
        val preparedForm = savedName.fold(form)(form.fill)

        Ok(view(existingId, preparedForm))
    }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(id, formWithErrors))),
        answer => {
          val currentPage   = LiaisonOfficerNamePage(id)
          val sessionUpdates = getSessionUpdates(currentPage, answer)

          userAnswersRepository
            .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
            .map(_ => Redirect(nextPage(currentPage, sessionUpdates)))
        }
      )
  }
}
