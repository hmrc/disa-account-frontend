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

package controllers.actions

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.when
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disaaccountfrontend.config.ErrorHandler
import uk.gov.hmrc.disaaccountfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.DataRetrievalActionImpl
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.LongTermAssetFunds
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.CashIsas
import uk.gov.hmrc.disaaccountfrontend.models.requests.{DataRequest, IdentifierRequest}
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import utils.BaseUnitSpec

import scala.concurrent.Future

class DataRetrievalActionSpec extends BaseUnitSpec {

  private val updatedOrgTelephoneNumber = "07777777777"
  private val updatedP2pPlatform        = "Updated platform"
  private val updatedP2pPlatformNumber  = "7654321"

  val errorHandler: ErrorHandler = app.injector.instanceOf[ErrorHandler]

  class Harness(
    registrationConnector: RegistrationConnector,
    userAnswersRepository: UserAnswersRepository,
    errorHandler: ErrorHandler
  ) extends DataRetrievalActionImpl(registrationConnector, userAnswersRepository, errorHandler) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  private def action = new Harness(mockRegistrationConnector, mockUserAnswersRepository, errorHandler)

  "DataRetrievalAction.refine" should {

    "convert registration details into effective answers when there are no session answers" in {
      val request = FakeRequest()
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetailsWithInnovativeFinanceIsa)))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(None))

      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result shouldBe Right(
        DataRequest(
          request,
          testZref,
          testCredentialId,
          testSessionId,
          sessionAnswers = None,
          effectiveAnswers = SessionUpdates(
            correspondenceAddress = Some(testCorrespondenceAddress),
            organisationTelephoneNumber = Some(testOrgTelephoneNumber),
            isaProducts = Some(testIsaProductSelections),
            innovativeFinancialProducts = Some(testInnovativeFinancialProductSelections),
            p2pPlatform = Some(testP2pPlatform),
            p2pPlatformNumber = Some(testP2pPlatformNumber)
          )
        )
      )
    }

    "prefer session answers field by field while falling back for unanswered fields" in {
      val request      = FakeRequest()
      val updates      = SessionUpdates(
        organisationTelephoneNumber = Some(updatedOrgTelephoneNumber),
        isaProducts = Some(Seq(CashIsas)),
        innovativeFinancialProducts = Some(Seq(LongTermAssetFunds)),
        p2pPlatform = Some(updatedP2pPlatform),
        p2pPlatformNumber = Some(updatedP2pPlatformNumber)
      )
      val savedAnswers = UserAnswers(testSessionId, updates)

      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetailsWithInnovativeFinanceIsa)))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(Some(savedAnswers)))

      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result shouldBe Right(
        DataRequest(
          request,
          testZref,
          testCredentialId,
          testSessionId,
          sessionAnswers = Some(savedAnswers),
          effectiveAnswers = SessionUpdates(
            correspondenceAddress = Some(testCorrespondenceAddress),
            organisationTelephoneNumber = Some(updatedOrgTelephoneNumber),
            isaProducts = Some(Seq(CashIsas)),
            innovativeFinancialProducts = Some(Seq(LongTermAssetFunds)),
            p2pPlatform = Some(updatedP2pPlatform),
            p2pPlatformNumber = Some(updatedP2pPlatformNumber)
          )
        )
      )
    }

    "retain explicitly empty session selections instead of restoring registration answers" in {
      val request      = FakeRequest()
      val savedAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(
          isaProducts = Some(Seq.empty),
          innovativeFinancialProducts = Some(Seq.empty)
        )
      )

      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetailsWithInnovativeFinanceIsa)))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(Some(savedAnswers)))

      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result.value.effectiveAnswers.isaProducts                 shouldBe Some(Seq.empty)
      result.value.effectiveAnswers.innovativeFinancialProducts shouldBe Some(Seq.empty)
    }

    "use empty effective answers when neither source has answers" in {
      val request = FakeRequest()
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(None))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(None))

      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result shouldBe Right(
        DataRequest(request, testZref, testCredentialId, testSessionId, None, SessionUpdates())
      )
    }

    "return an InternalServerError and not admit the request when the call to disa-account fails" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.failed(new RuntimeException("Boom")))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(None))

      val result = await(
        action.callRefine(IdentifierRequest(FakeRequest(), testZref, testCredentialId, testSessionId))
      )

      result.isLeft                                shouldBe true
      status(Future.successful(result.left.value)) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an InternalServerError and not admit the request when session retrieval fails" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetails)))
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.failed(new RuntimeException("Boom")))

      val result = await(
        action.callRefine(IdentifierRequest(FakeRequest(), testZref, testCredentialId, testSessionId))
      )

      result.isLeft                                shouldBe true
      status(Future.successful(result.left.value)) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
