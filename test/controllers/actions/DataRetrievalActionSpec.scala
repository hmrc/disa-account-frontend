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
import uk.gov.hmrc.disaaccountfrontend.models.requests.{DataRequest, IdentifierRequest}
import utils.BaseUnitSpec

import scala.concurrent.Future

class DataRetrievalActionSpec extends BaseUnitSpec {

  val errorHandler: ErrorHandler = app.injector.instanceOf[ErrorHandler]

  class Harness(registrationConnector: RegistrationConnector, errorHandler: ErrorHandler)
      extends DataRetrievalActionImpl(registrationConnector, errorHandler) {
    def callRefine[A](request: IdentifierRequest[A]): Future[Either[Result, DataRequest[A]]] = refine(request)
  }

  "DataRetrievalAction.refine" should {

    "carry forward the registration details fetched from disa-account" in {
      val request = FakeRequest()
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetails)))

      val action = new Harness(mockRegistrationConnector, errorHandler)
      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result shouldBe Right(
        DataRequest(request, testZref, testCredentialId, testSessionId, Some(testRegistrationDetails))
      )
    }

    "carry forward None when disa-account has nothing to prefill" in {
      val request = FakeRequest()
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(None))

      val action = new Harness(mockRegistrationConnector, errorHandler)
      val result = action
        .callRefine(IdentifierRequest(request, testZref, testCredentialId, testSessionId))
        .futureValue

      result shouldBe Right(DataRequest(request, testZref, testCredentialId, testSessionId, None))
    }

    "return an InternalServerError and not admit the request when the call to disa-account fails" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.failed(new RuntimeException("Boom")))

      val action = new Harness(mockRegistrationConnector, errorHandler)
      val result = action
        .callRefine(IdentifierRequest(FakeRequest(), testZref, testCredentialId, testSessionId))
        .futureValue

      result.isLeft shouldBe true
      status(Future.successful(result.left.value)) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
