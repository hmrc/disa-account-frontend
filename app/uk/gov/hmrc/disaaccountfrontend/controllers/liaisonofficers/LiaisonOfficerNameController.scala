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
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.PageController
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.LiaisonOfficerNameFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficers.findLiaisonOfficer
import uk.gov.hmrc.disaaccountfrontend.models.{Mode, NormalMode, UserAnswers}
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
  guardPage: PageGuardAction,
  userAnswersRepository: UserAnswersRepository,
  appConfig: AppConfig,
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

  private def page(id: String): LiaisonOfficerNamePage =
    LiaisonOfficerNamePage(id)

  private def pageAction(page: LiaisonOfficerNamePage) =
    identify andThen getData andThen guardPage(page, appConfig)

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] = {
    val currentPage = page(id.getOrElse(uuidGenerator.generate()))

    pageAction(currentPage) { implicit request =>
      id match {
        case None if mode == NormalMode =>
          Redirect(routes.LiaisonOfficerNameController.onPageLoad(Some(currentPage.id), mode))
        case Some(existingId)           =>
          val savedName    = findLiaisonOfficer(existingId).flatMap(_.fullName)
          val preparedForm = savedName.fold(form)(form.fill)

          Ok(view(existingId, mode, preparedForm))
        case _                          => Redirect(ChangeOfCircumstancesController.onPageLoad())
      }
    }
  }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = {
    val currentPage = page(id)

    pageAction(currentPage).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, mode, formWithErrors))),
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
