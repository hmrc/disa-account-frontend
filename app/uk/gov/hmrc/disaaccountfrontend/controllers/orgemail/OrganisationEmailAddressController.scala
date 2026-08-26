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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.OrganisationEmailAddressFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.OrganisationEmailAddressPage
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.orgemail.OrganisationEmailAddressView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class OrganisationEmailAddressController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: OrganisationEmailAddressFormProvider,
  emailVerificationConnector: EmailVerificationConnector,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: OrganisationEmailAddressView
)(implicit ec: ExecutionContext)
    extends PageController(OrganisationEmailAddressPage, navigator)
    with FrontendBaseController
    with I18nSupport
    with Logging {

  private val form       = formProvider()
  private val pageAction = identify andThen getData

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    val preparedForm = request.effectiveAnswers.organisationEmailAddress.fold(form)(form.fill)
    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        answer =>
          emailVerificationConnector
            .sendCode(answer)
            .flatMap { _ =>
              val sessionUpdates = getSessionUpdates(answer)

              userAnswersRepository
                .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
                .map(_ => Redirect(nextPage(sessionUpdates)))
            }
            .recoverWith { case NonFatal(e) =>
              logger.error(
                s"[OrganisationEmailAddressController][onSubmit] Failed to send email verification code for zref: [${request.zReference}]",
                e
              )
              errorHandler.internalServerError
            }
      )
  }
}
