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

package controllers.orgdetails

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class OrganisationTelephoneNumberControllerSpec extends BaseUnitSpec {

  // prod.routes mounts app.routes under this prefix, which the per-controller reverse router
  // (uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes) doesn't know about, so it's hardcoded here.
  val onPageLoadUrl: String = "/obligations/account/isa/organisation-telephone-number"
  val onSubmitUrl: String   = "/obligations/account/isa/organisation-telephone-number"

  val validFormData: Map[String, String] = Map("value" -> "01642123456")

  "OrganisationTelephoneNumberController.onPageLoad" should {

    "return 200 OK prefilled from the effective answers supplied by the retrieval action" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(organisationTelephoneNumber = Some("01642123456"))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result)        shouldBe OK
        contentAsString(result) should include("01642123456")
      }
    }

    "return 200 OK with an empty form when there is nothing to prefill" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, onPageLoadUrl)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include "01642123456"
      }
    }
  }

  "OrganisationTelephoneNumberController.onSubmit" should {

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

    "preserve an existing cached correspondence address when saving the answer" in {
      when(mockUserAnswersRepository.get(testSessionId))
        .thenReturn(
          Future.successful(
            Some(UserAnswers(testSessionId, SessionUpdates(correspondenceAddress = Assign(testCorrespondenceAddress))))
          )
        )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, onSubmitUrl)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Assign("01642123456")
        )
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, onSubmitUrl)
            .withFormUrlEncodedBody("value" -> "")
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
