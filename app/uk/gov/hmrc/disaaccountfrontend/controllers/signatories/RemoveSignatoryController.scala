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

import play.api.data.Form
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.disaaccountfrontend.controllers.PageController
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.{UserAnswers, YesNoAnswer}
import uk.gov.hmrc.disaaccountfrontend.models.pages.signatories.RemoveSignatoryPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.RemoveSignatoryView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveSignatoryController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: YesNoAnswerFormProvider,
  val controllerComponents: MessagesControllerComponents,
  val signatory: Signatory,
  view: RemoveSignatoryView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport {

  private val form: Form[YesNoAnswer] = formProvider("removeSignatory.error.required")
  private val pageAction              = identify andThen getData // andThen guardPage(RemoveSignatoryPage)

  def onPageLoad(id: String): Action[AnyContent] = pageAction.async { implicit request =>
    providingName(id, name => Future.successful(Ok(view(id, name, form))))

  }

  def onSubmit(id: String): Action[AnyContent] = pageAction.async { implicit request =>
    providingName(
      id,
      name =>
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(id, name, formWithErrors))),
            value => {

              val sessionUpdates = getSessionUpdates(RemoveSignatoryPage.apply(id), value)

              userAnswersRepository.set(UserAnswers(id = request.sessionId, updates = sessionUpdates)).map { _ =>
                Redirect(nextPage(RemoveSignatoryPage(id), sessionUpdates))
              }

            }
          )
    )

  }

  private def findSignatory(id: String)(implicit request: DataRequest[_]): Option[Signatory] =
    request.effectiveAnswers.signatories.flatMap(_.signatories.find(_.id == id))

  private def providingName(id: String, block: String => Future[Result])(implicit request: DataRequest[_]) =
    (for {
      signatory <- findSignatory(id)
      name      <- signatory.fullName
    } yield block(name))
      .getOrElse(Future.successful(Redirect(ChangeOfCircumstancesController.onPageLoad())))

}
