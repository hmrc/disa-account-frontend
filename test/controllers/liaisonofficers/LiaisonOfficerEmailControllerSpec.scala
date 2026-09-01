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

package controllers.liaisonofficers

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class LiaisonOfficerEmailControllerSpec extends BaseUnitSpec {

  private val existingId   = "existing-id"
  private val existingName = "Joe Bloggs"

  private val existingOfficer = LiaisonOfficer(
    id = existingId,
    fullName = Some(existingName),
    phoneNumber = Some("07777777777"),
    communication = Set(ByEmail, ByPhone),
    email = Some("old@example.com")
  )

  private val otherOfficer = LiaisonOfficer(
    id = "other-id",
    fullName = Some("Other Person"),
    email = Some("other@example.com")
  )

  private val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, existingOfficer))))

  "LiaisonOfficerEmailController.onPageLoad" should {

    "render the page with the liaison officer name and standard service content in normal mode" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerEmailEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                    shouldBe OK
        doc.title()                       shouldBe
          "What is the email address of Joe Bloggs? - Manage ISAs - GOV.UK"
        doc.select("h1").text()             should include("What is the email address of Joe Bloggs?")
        doc.text()                          should include("This is the email address that HMRC will use to contact the liaison officer")
        doc.select("form").attr("action") shouldBe liaisonOfficerEmailEndpointFor(existingId)
        doc.select("button").text()       shouldBe "Continue"
      }
    }

    "use the check-mode form action when arriving from check your answers" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeLiaisonOfficerEmailEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                    shouldBe OK
        doc.select("form").attr("action") shouldBe changeLiaisonOfficerEmailEndpointFor(existingId)
      }
    }

    "repopulate the saved email for the matching liaison officer id" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerEmailEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                shouldBe OK
        doc.select("input[name=value]").attr("value") shouldBe "old@example.com"
      }
    }

    "render an empty field when the liaison officer has no saved email" in {
      val answersWithoutEmail = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(existingOfficer.copy(email = None))))
      )
      val application         = applicationBuilder(effectiveAnswers = answersWithoutEmail).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerEmailEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                shouldBe OK
        doc.select("input[name=value]").attr("value") shouldBe empty
      }
    }

    "redirect to change of circumstances when the identified liaison officer does not exist" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerEmailEndpointFor(existingId))).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "redirect to change of circumstances when the identified liaison officer has no name" in {
      val officerWithoutName = existingOfficer.copy(fullName = None)
      val application        = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officerWithoutName))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerEmailEndpointFor(existingId))).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }

  "LiaisonOfficerEmailController.onSubmit" should {

    "trim and save the email in the logged-in session while preserving existing answers and officer details" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      val application     = applicationBuilder(
        effectiveAnswers = answers,
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerEmailEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "  updated@example.com  ")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, existingOfficer.copy(email = Some("updated@example.com"))))
          )
        )
      }
    }

    "save and use the check-mode fallback when submitted from check your answers" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, changeLiaisonOfficerEmailEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "updated@example.com")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "return the required error when the field is blank" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerEmailEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "   ")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        doc.text()       should include(
          "Enter the email address of the liaison officer you’re adding. " +
            "Use a name, @ symbol and a domain name, like yourname@example.com"
        )
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return the invalid-format error when the email address is incorrect" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerEmailEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "not-an-email")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        doc.text()       should include(
          "Enter an email address in the correct format. " +
            "Use a name, @ symbol and a domain name, like yourname@example.com"
        )
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect without saving when the guard fails" in {
      val application = applicationBuilder(effectiveAnswers = Answers()).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerEmailEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "updated@example.com")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
