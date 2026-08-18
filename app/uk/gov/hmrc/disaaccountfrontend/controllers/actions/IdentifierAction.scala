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

package uk.gov.hmrc.disaaccountfrontend.controllers.actions

import play.api.Logging
import play.api.mvc.Results.Redirect
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.routes
import uk.gov.hmrc.disaaccountfrontend.models.requests.IdentifierRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait IdentifierAction
    extends ActionBuilder[IdentifierRequest, AnyContent]
    with ActionFunction[Request, IdentifierRequest]

class AuthenticatedIdentifierAction @Inject() (
  override val authConnector: AuthConnector,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends IdentifierAction
    with AuthorisedFunctions
    with Logging {

  private val enrolmentKey  = config.manageIsaEnrolmentKey
  private val identifierKey = config.zrefIdentifierKey

  override def invokeBlock[A](request: Request[A], block: IdentifierRequest[A] => Future[Result]): Future[Result] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised(Enrolment(enrolmentKey)).retrieve(authorisedEnrolments and credentials) {
      case enrolments ~ credentials =>
        val zref      = enrolments.getEnrolment(enrolmentKey).flatMap(_.getIdentifier(identifierKey)).map(_.value)
        val sessionId = hc.sessionId.map(_.value)

        (zref, credentials, sessionId) match {
          case (Some(zref), Some(creds), Some(sessionId)) =>
            block(IdentifierRequest(request, zref, creds.providerId, sessionId))
          case (None, _, _)                               =>
            logger.warn(
              s"[AuthenticatedIdentifierAction][invokeBlock] User with enrolment [$enrolmentKey] was missing identifier [$identifierKey]"
            )
            Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
          case (_, None, _)                               =>
            logger.warn("[AuthenticatedIdentifierAction][invokeBlock] User with DISA enrolment was missing credentials")
            Future.successful(Redirect(routes.UnauthorisedController.onPageLoad()))
          case (_, _, None)                               =>
            logger.warn("[AuthenticatedIdentifierAction][invokeBlock] Request was missing a session ID")
            Future.successful(Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl))))
        }
    } recover {
      case _: NoActiveSession        =>
        Redirect(config.loginUrl, Map("continue" -> Seq(config.loginContinueUrl)))
      case _: AuthorisationException =>
        Redirect(routes.UnauthorisedController.onPageLoad())
    }
  }
}
