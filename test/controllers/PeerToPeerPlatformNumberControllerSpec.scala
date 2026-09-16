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

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class PeerToPeerPlatformNumberControllerSpec extends BaseUnitSpec {

  private val numberFieldName           = "value"
  private val csrfHeaderName            = "Csrf-Token"
  private val csrfHeaderValue           = "nocheck"
  private val hintMessage               = "The FCA, FRN is usually a 6 or 7 digit number"
  private val previousP2pPlatformNumber = "7654321"

  private val invalidCharactersError =
    "The FCA must not include letters a to z, hyphens, spaces or apostrophes"
  private val patternError           =
    "The FCA should be 6 or 7 digits without letters, hyphens, spaces or other characters"

  private def expectedTitle(platformName: String)         = s"What is the FCA, FRN of $platformName?"
  private def expectedHeading(platformName: String)       = expectedTitle(platformName)
  private def expectedRequiredError(platformName: String) = s"Enter the FCA number of $platformName"

  private val eligibleAnswers = Answers(p2pPlatform = Some(testP2pPlatform))

  "PeerToPeerPlatformNumberController.onPageLoad" should {

    "render the page with the platform name from the previous answer and pre-populate a saved number" in {
      implicit val application: Application = applicationBuilder(
        effectiveAnswers = eligibleAnswers.copy(p2pPlatformNumber = Some(testP2pPlatformNumber))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, peerToPeerPlatformNumberEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                      shouldBe OK
        doc.title()                                           should include(expectedTitle(testP2pPlatform))
        doc.select(s"input#$numberFieldName").attr("value") shouldBe testP2pPlatformNumber
        doc.select("h1").text()                               should include(expectedHeading(testP2pPlatform))
        doc.text()                                            should include(hintMessage)
      }
    }

    "redirect when the platform name has not been answered" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, peerToPeerPlatformNumberEndpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
      }
    }
  }

  "PeerToPeerPlatformNumberController.onSubmit" should {

    "return the exact required error, including the platform name, and not save when the number is blank" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint).withHeaders(csrfHeaderName -> csrfHeaderValue)
        ).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(expectedRequiredError(testP2pPlatform))
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return the invalid characters error and not save when the number contains a letter" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(numberFieldName -> "12A4567")
        ).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(invalidCharactersError)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return the invalid characters error when the number contains a hyphen, space or apostrophe" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        Seq("123-4567", "123 4567", "123'4567").foreach { value =>
          val result = route(
            application,
            FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
              .withHeaders(csrfHeaderName -> csrfHeaderValue)
              .withFormUrlEncodedBody(numberFieldName -> value)
          ).value
          val doc    = Jsoup.parse(contentAsString(result))

          status(result)                          shouldBe BAD_REQUEST
          doc.select(".govuk-error-message").text() should include(invalidCharactersError)
        }
      }
    }

    "return the pattern error and not save when the number is the wrong length" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(numberFieldName -> "12345")
        ).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(patternError)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return the pattern error when the number contains a character that is not a letter, hyphen, space or apostrophe" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(numberFieldName -> "123456!")
        ).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(patternError)
      }
    }

    "save the platform number, and not the platform name, while preserving other session changes" in {
      val existingAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Assign(testOrgTelephoneNumber),
          p2pPlatform = Assign(testP2pPlatform),
          p2pPlatformNumber = Assign(previousP2pPlatformNumber)
        )
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = eligibleAnswers.copy(p2pPlatformNumber = Some(previousP2pPlatformNumber)),
        sessionAnswers = Some(existingAnswers)
      ).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(numberFieldName -> testP2pPlatformNumber)
        ).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Assign(testOrgTelephoneNumber),
          p2pPlatform = Assign(testP2pPlatform),
          p2pPlatformNumber = Assign(testP2pPlatformNumber)
        )
      }
    }

    "redirect without saving when the platform name has not been answered" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformNumberEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(numberFieldName -> testP2pPlatformNumber)
        ).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
