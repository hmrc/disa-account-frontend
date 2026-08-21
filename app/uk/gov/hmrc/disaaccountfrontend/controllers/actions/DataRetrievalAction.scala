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
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.disaaccountfrontend.config.ErrorHandler
import uk.gov.hmrc.disaaccountfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import uk.gov.hmrc.disaaccountfrontend.models.requests.{DataRequest, IdentifierRequest}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

trait DataRetrievalAction extends ActionRefiner[IdentifierRequest, DataRequest]

class DataRetrievalActionImpl @Inject() (
  registrationConnector: RegistrationConnector,
  userAnswersRepository: UserAnswersRepository,
  errorHandler: ErrorHandler
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with Logging {

  override protected def refine[A](request: IdentifierRequest[A]): Future[Either[Result, DataRequest[A]]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val registrationDetails = registrationConnector.getRegistrationDetails(request.zReference)
    val sessionAnswers      = userAnswersRepository.get(request.sessionId)

    registrationDetails
      .zip(sessionAnswers)
      .map { case (registrationDetails, sessionAnswers) =>
        Right(
          DataRequest(
            request.request,
            request.zReference,
            request.credentialId,
            request.sessionId,
            sessionAnswers,
            mergeAnswers(registrationDetails, sessionAnswers)
          )
        )
      }
      .recoverWith { case NonFatal(e) =>
        logger.error(
          s"[DataRetrievalActionImpl][refine] Failed to retrieve answers for zref: [${request.zReference}]",
          e
        )
        errorHandler.internalServerError(request).map(Left.apply)
      }
  }

  private def mergeAnswers(
    registrationDetails: Option[RegistrationDetails],
    sessionAnswers: Option[UserAnswers]
  ): Answers = {
    val registrationAnswers = Answers(
      correspondenceAddress = registrationDetails.flatMap(_.correspondenceAddress),
      organisationTelephoneNumber = registrationDetails.flatMap(_.orgTelephoneNumber),
      tradingName = registrationDetails.flatMap(_.tradingName),
      isaProducts = registrationDetails.flatMap(_.isaProductSelections),
      innovativeFinancialProducts = registrationDetails.flatMap(_.innovativeFinancialProductSelections),
      p2pPlatform = registrationDetails.flatMap(_.p2pPlatform),
      p2pPlatformNumber = registrationDetails.flatMap(_.p2pPlatformNumber),
      financialOrganisation = registrationDetails.flatMap(_.financialOrganisation)
    )

    sessionAnswers.fold(registrationAnswers)(_.updates.getUpdatedEffectiveAnswers(registrationAnswers))
  }
}
