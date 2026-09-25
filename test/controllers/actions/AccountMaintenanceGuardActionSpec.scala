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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.when
import play.api.mvc.AnyContentAsEmpty
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.AccountMaintenanceGuardActionImpl
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import utils.BaseUnitSpec

import scala.concurrent.Future

class AccountMaintenanceGuardActionSpec extends BaseUnitSpec {

  private val action = new AccountMaintenanceGuardActionImpl(mockRegistrationConnector)

  "AccountMaintenanceGuardAction" should {

    "continue with the request when no ISA product change is under review" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetails)))

      val result = action.invokeBlock(dataRequest, _ => Future.successful(Ok))

      status(result) shouldBe OK
    }

    "continue with the request when disa-account has no registration details" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(None))

      val result = action.invokeBlock(dataRequest, _ => Future.successful(Ok))

      status(result) shouldBe OK
    }

    "redirect to manage ISAs when an ISA product change is under review" in {
      when(mockRegistrationConnector.getRegistrationDetails(eqTo(testZref))(any()))
        .thenReturn(Future.successful(Some(testRegistrationDetails.copy(isaProductsChangeUnderReview = true))))

      val result = action.invokeBlock(dataRequest, _ => Future.successful(Ok))

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) should contain(manageIsasEndpoint)
    }
  }

  private def dataRequest: DataRequest[AnyContentAsEmpty.type] =
    DataRequest(
      FakeRequest(),
      testZref,
      testCredentialId,
      None,
      testSessionId,
      sessionAnswers = None,
      originalAnswers = Answers(),
      effectiveAnswers = Answers()
    )
}
