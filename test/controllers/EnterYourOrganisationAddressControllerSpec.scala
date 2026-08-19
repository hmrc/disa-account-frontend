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

package controllers

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import utils.BaseUnitSpec

import scala.concurrent.Future

class EnterYourOrganisationAddressControllerSpec extends BaseUnitSpec {

  // prod.routes mounts app.routes under this prefix, which the per-controller reverse router
  // (uk.gov.hmrc.disaaccountfrontend.controllers.routes) doesn't know about, so it's hardcoded here.
  val onPageLoadUrl: String = "/obligations/account/isa/enter-your-organisation-address"
  val onSubmitUrl: String   = "/obligations/account/isa/enter-your-organisation-address"

  val validFormData: Map[String, String] = Map(
    "addressLine1" -> "1 Test Street",
    "townOrCity"   -> "Test Town",
    "postcode"     -> "AA1 1AA"
  )

  "EnterYourOrganisationAddressController.onPageLoad" should {

    "return 200 OK prefilled from the effective answers supplied by the retrieval action" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(correspondenceAddress = Some(testCorrespondenceAddress))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result)        shouldBe OK
        contentAsString(result) should include("1 Test Street")
      }
    }

    "return 200 OK with an empty form when there is nothing to prefill" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include "1 Test Street"
      }
    }
  }

  "EnterYourOrganisationAddressController.onSubmit" should {

    "save the answer and redirect when the form is valid" in {
      when(mockUserAnswersRepository.get(testSessionId)).thenReturn(Future.successful(None))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, onSubmitUrl)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, onSubmitUrl)
            .withFormUrlEncodedBody(validFormData.updated("addressLine1", "").toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
