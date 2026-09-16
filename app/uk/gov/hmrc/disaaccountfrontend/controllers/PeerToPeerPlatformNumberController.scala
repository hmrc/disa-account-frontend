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

import play.api.data.Form
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.PeerToPeerPlatformNumberFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.PeerToPeerPlatformNumberPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.PeerToPeerPlatformNumberView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PeerToPeerPlatformNumberController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: PeerToPeerPlatformNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PeerToPeerPlatformNumberView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport {

  private val pageAction = identify andThen getData andThen guardPage(PeerToPeerPlatformNumberPage)

  private def platformName(implicit request: DataRequest[_]): String =
    request.effectiveAnswers.p2pPlatform.get

  private def form(platformName: String)(implicit messages: Messages): Form[String] =
    formProvider(platformName)

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    val platform     = platformName
    val preparedForm = request.effectiveAnswers.p2pPlatformNumber.fold(form(platform))(form(platform).fill)

    Ok(view(preparedForm, platform))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    val platform = platformName

    form(platform)
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, platform))),
        answer => {
          val sessionUpdates = getSessionUpdates(PeerToPeerPlatformNumberPage, answer)

          userAnswersRepository
            .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
            .map(_ => Redirect(nextPage(PeerToPeerPlatformNumberPage, sessionUpdates)))
        }
      )
  }
}
