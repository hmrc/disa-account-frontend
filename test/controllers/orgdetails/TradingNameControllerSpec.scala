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

class TradingNameControllerSpec extends BaseUnitSpec {

  val testTradingName: String = "Acme Savings Ltd"

  val validFormData: Map[String, String] = Map("value" -> testTradingName)

  "TradingNameController.onPageLoad" should {

    "return 200 OK prefilled from the effective answers supplied by the retrieval action" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(tradingName = Some(testTradingName))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, tradingNameEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testTradingName)
      }
    }

    "return 200 OK with an empty form when there is nothing to prefill" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, tradingNameEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include testTradingName
      }
    }
  }

  "TradingNameController.onSubmit" should {

    "save the answer and redirect when the form is valid" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "preserve an existing cached correspondence address when saving the answer" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        sessionAnswers =
          Some(UserAnswers(testSessionId, SessionUpdates(correspondenceAddress = Assign(testCorrespondenceAddress))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          tradingName = Assign(testTradingName)
        )
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, tradingNameEndpoint)
            .withFormUrlEncodedBody("value" -> "")
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
