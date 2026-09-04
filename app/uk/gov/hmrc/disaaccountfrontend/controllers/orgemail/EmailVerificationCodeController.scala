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

package uk.gov.hmrc.disaaccountfrontend.controllers.orgemail

import play.api.Logging
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.config.ErrorHandler
import uk.gov.hmrc.disaaccountfrontend.connectors.EmailVerificationConnector
import uk.gov.hmrc.disaaccountfrontend.controllers.PageController
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.orgemail.routes.EmailVerificationCodeController as EmailVerificationCodeRoutes
import uk.gov.hmrc.disaaccountfrontend.controllers.orgemail.routes.OrganisationEmailAddressController as OrganisationEmailAddressRoutes
import uk.gov.hmrc.disaaccountfrontend.forms.generic.EmailVerificationCodeFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Clear
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.SessionUpdates
import uk.gov.hmrc.disaaccountfrontend.models.emailverification.VerifyEmailCodeResult
import uk.gov.hmrc.disaaccountfrontend.models.pages.EmailVerificationCodePage
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.orgemail.EmailVerificationCodeView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EmailVerificationCodeController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: EmailVerificationCodeFormProvider,
  emailVerificationConnector: EmailVerificationConnector,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: EmailVerificationCodeView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport
    with Logging {

  private val form       = formProvider()
  private val pageAction =
    identify andThen getData andThen guardPage(
      EmailVerificationCodePage,
      OrganisationEmailAddressRoutes.onPageLoad()
    )

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    Ok(view(form, request.effectiveAnswers.organisationEmailAddress.get))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    val email = request.effectiveAnswers.organisationEmailAddress.get

    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, email))),
        code =>
          emailVerificationConnector
            .verifyCode(email, code)
            .flatMap {
              case VerifyEmailCodeResult.Verified    =>
                val sessionUpdates = getSessionUpdates(EmailVerificationCodePage, true)
                userAnswersRepository
                  .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
                  .map(_ => Redirect(nextPage(EmailVerificationCodePage, sessionUpdates)))
              case VerifyEmailCodeResult.InvalidCode =>
                Future
                  .successful(BadRequest(view(form.withError("value", "emailVerificationCode.error.invalid"), email)))
            }
            .recoverWith { case NonFatal(e) =>
              logger.error(
                s"[EmailVerificationCodeController][onSubmit] Failed to verify email confirmation code for zref: [${request.zReference}]",
                e
              )
              errorHandler.internalServerError
            }
      )
  }

  def requestNewCode(): Action[AnyContent] = pageAction.async { implicit request =>
    val email           = request.effectiveAnswers.organisationEmailAddress.get
    val existingUpdates = request.sessionAnswers.fold(SessionUpdates())(_.updates)
    val sessionUpdates  = existingUpdates.copy(organisationEmailVerified = Clear)

    emailVerificationConnector
      .sendCode(email)
      .flatMap { _ =>
        userAnswersRepository
          .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
          .map(_ => Redirect(EmailVerificationCodeRoutes.onPageLoad()))
      }
      .recoverWith { case NonFatal(e) =>
        logger.error(
          s"[EmailVerificationCodeController][requestNewCode] Failed to request a new email confirmation code for zref: [${request.zReference}]",
          e
        )
        errorHandler.internalServerError
      }
  }
}
