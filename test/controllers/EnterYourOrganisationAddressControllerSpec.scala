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

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CorrespondenceAddress, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class EnterYourOrganisationAddressControllerSpec extends BaseUnitSpec {

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
        val result = route(application, FakeRequest(GET, enterYourOrganisationAddressEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should include("1 Test Street")
      }
    }

    "return 200 OK with an empty form when there is nothing to prefill" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, enterYourOrganisationAddressEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include "1 Test Street"
      }
    }
  }

  "EnterYourOrganisationAddressController.onSubmit" should {

    "save the answer, preserve existing session changes and redirect when the form is valid" in {
      val existingAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = Answers(organisationTelephoneNumber = Some(testOrgTelephoneNumber)),
        sessionAnswers = Some(existingAnswers)
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourOrganisationAddressEndpoint)
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(organisationTelephoneNumberEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe SessionUpdates(
          correspondenceAddress = Assign(
            CorrespondenceAddress(
              addressLine1 = Some("1 Test Street"),
              addressLine3 = Some("Test Town"),
              postCode = Some("AA1 1AA")
            )
          ),
          organisationTelephoneNumber = Assign(testOrgTelephoneNumber)
        )
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, enterYourOrganisationAddressEndpoint)
            .withFormUrlEncodedBody(validFormData.updated("addressLine1", "").toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
