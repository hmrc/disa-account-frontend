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

import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.forms.SignatoryNameFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.disaaccountfrontend.models.{CheckMode, Mode, NormalMode, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryNamePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.SignatoryNameView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignatoryNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: SignatoryNameFormProvider,
  appConfig: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String], mode: Mode): Action[AnyContent] = (identify andThen getData) { implicit request =>
    def renderPage(id: String, signatory: Option[Signatory]) = {
      val preparedForm = signatory.flatMap(_.fullName).fold(form)(form.fill)
      Ok(view(id, mode, preparedForm))
    }

    (mode, id, findSignatory(id, request)) match {
      case (NormalMode, None, None)                        =>
        if (signatoryCount(request) >= appConfig.maxSignatories) {
          Redirect(ChangeOfCircumstancesController.onPageLoad())
        } else {
          Redirect(routes.SignatoryNameController.onPageLoad(Some(UUID.randomUUID().toString), mode))
        }
      case (NormalMode, Some(existingId), Some(signatory)) =>
        renderPage(existingId, Some(signatory))
      case (NormalMode, Some(newId), None)                 =>
        renderPage(newId, None)
      case (CheckMode, Some(existingId), Some(signatory))  =>
        renderPage(existingId, Some(signatory))
      case _                                               =>
        Redirect(ChangeOfCircumstancesController.onPageLoad())
    }
  }

  def onSubmit(id: String, mode: Mode): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(id, mode, formWithErrors))),
        answer => {
          val isNewSignatory = findSignatory(Some(id), request).isEmpty

          if (isNewSignatory && (mode != NormalMode || signatoryCount(request) >= appConfig.maxSignatories)) {
            Future.successful(Redirect(ChangeOfCircumstancesController.onPageLoad()))
          } else {
            val sessionUpdates = SignatoryNamePage(id, mode).saveAnswerAndHandleDependents(request, answer)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
              .map { _ =>
                Redirect(
                  navigator.nextPage(
                    SignatoryNamePage(id, mode),
                    sessionUpdates.getUpdatedEffectiveAnswers(request.effectiveAnswers),
                    mode
                  )
                )
              }
          }
        }
      )
  }

  private def signatoryCount(request: DataRequest[_]): Int =
    request.effectiveAnswers.signatories.map(_.signatories.size).getOrElse(0)

  private def findSignatory(id: Option[String], request: DataRequest[_]): Option[Signatory] =
    for {
      existingId <- id
      signatory  <- request.effectiveAnswers.signatories.flatMap(_.signatories.find(_.id == existingId))
    } yield signatory
}
