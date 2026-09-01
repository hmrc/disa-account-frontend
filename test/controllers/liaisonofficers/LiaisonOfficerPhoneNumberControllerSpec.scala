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

class LiaisonOfficerPhoneNumberControllerSpec extends BaseUnitSpec {

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
    phoneNumber = Some("01642123456"),
    email = Some("other@example.com")
  )

  private val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, existingOfficer))))

  "LiaisonOfficerPhoneNumberController.onPageLoad" should {

    "render the page with the liaison officer name and standard service content in normal mode" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerPhoneNumberEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)        shouldBe OK
        doc.title()           shouldBe "What is Joe Bloggs’s phone number? - Manage ISAs - GOV.UK"
        doc.select("h1").text() should include("What is Joe Bloggs’s phone number?")
        doc.text()              should include(
          "This is the phone number that HMRC will use if there is a need to contact the liaison officer."
        )
        doc.text()              should include("This can be either a UK mobile or landline number")
        doc.text()              should not include "Liaison officers"
        val formGroup = doc.select(".govuk-form-group").first()
        formGroup.child(0).select("label").text()                shouldBe "What is Joe Bloggs’s phone number?"
        formGroup.child(1).tagName()                             shouldBe "p"
        formGroup.child(1).text()                                shouldBe
          "This is the phone number that HMRC will use if there is a need to contact the liaison officer."
        formGroup.child(2).id()                                  shouldBe "value-hint"
        formGroup.child(2).text()                                shouldBe "This can be either a UK mobile or landline number"
        formGroup.child(3).tagName()                             shouldBe "input"
        doc.select("form").attr("action")                        shouldBe liaisonOfficerPhoneNumberEndpointFor(existingId)
        doc.select("input[name=value]").attr("inputmode")        shouldBe "numeric"
        doc.select("input[name=value]").attr("aria-describedby") shouldBe "value-hint"
        doc.select("button").text()                              shouldBe "Continue"
      }
    }

    "use the check-mode form action when arriving from check your answers" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeLiaisonOfficerPhoneNumberEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                    shouldBe OK
        doc.select("form").attr("action") shouldBe changeLiaisonOfficerPhoneNumberEndpointFor(existingId)
      }
    }

    "repopulate the saved phone number for the matching liaison officer id" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerPhoneNumberEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                shouldBe OK
        doc.select("input[name=value]").attr("value") shouldBe "07777777777"
      }
    }

    "render an empty field when the liaison officer has no saved phone number" in {
      val answersWithoutPhoneNumber = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(existingOfficer.copy(phoneNumber = None))))
      )
      val application               = applicationBuilder(effectiveAnswers = answersWithoutPhoneNumber).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerPhoneNumberEndpointFor(existingId))).value
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
        val result = route(application, FakeRequest(GET, liaisonOfficerPhoneNumberEndpointFor(existingId))).value

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
        val result = route(application, FakeRequest(GET, liaisonOfficerPhoneNumberEndpointFor(existingId))).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }

  "LiaisonOfficerPhoneNumberController.onSubmit" should {

    "normalise and save the phone number in the logged-in session while preserving existing answers" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      val application     = applicationBuilder(
        effectiveAnswers = answers,
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerPhoneNumberEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "07777 777 777")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, existingOfficer.copy(phoneNumber = Some("07777777777"))))
          )
        )
      }
    }

    "save and use the check-mode fallback when submitted from check your answers" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, changeLiaisonOfficerPhoneNumberEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "07123456789")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "return the registration error and not save invalid input" in {
      val invalidInputs = Seq(
        "   "          -> "Enter the phone number of the liaison officer you’re adding. Use a UK phone number, like 01642 123 456 or 07777 777 777",
        "0164212345a"  -> "The phone number must not include letters a to z, hyphens or apostrophes and must be a UK phone number, like 01642 123 456 or 07777 777 777",
        "016421234567" -> "The phone number you have entered is too long. Enter a UK phone number, like 01642 123 456 or 07777 777 777",
        "0164212345"   -> "The phone number you have entered is too short. Enter a UK phone number, like 01642 123 456 or 07777 777 777"
      )

      invalidInputs.foreach { case (input, expectedError) =>
        val application = applicationBuilder(effectiveAnswers = answers).build()

        running(application) {
          val request = FakeRequest(POST, liaisonOfficerPhoneNumberEndpointFor(existingId))
            .withFormUrlEncodedBody("value" -> input)
            .withHeaders("Csrf-Token" -> "nocheck")
          val result  = route(application, request).value

          status(result)        shouldBe BAD_REQUEST
          contentAsString(result) should include(expectedError)
          verify(mockUserAnswersRepository, never).set(any())
        }
      }
    }

    "render the paragraph, hint and error in an accessible order" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerPhoneNumberEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST

        val formGroup = doc.select(".govuk-form-group").first()
        formGroup.hasClass("govuk-form-group--error") shouldBe true
        formGroup.child(0).select("label").text()     shouldBe "What is Joe Bloggs’s phone number?"
        formGroup.child(1).tagName()                  shouldBe "p"
        formGroup.child(2).id()                       shouldBe "value-hint"
        formGroup.child(3).id()                       shouldBe "value-error"
        formGroup.child(4).tagName()                  shouldBe "input"

        val input = formGroup.select("input[name=value]")
        input.hasClass("govuk-input--error") shouldBe true
        input.attr("aria-describedby")       shouldBe "value-hint value-error"
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect without saving when the guard fails" in {
      val application = applicationBuilder(effectiveAnswers = Answers()).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerPhoneNumberEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "07123456789")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
