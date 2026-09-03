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
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone, ByPost}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class LiaisonOfficerCommunicationControllerSpec extends BaseUnitSpec {

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
    communication = Set(ByPost),
    email = Some("other@example.com")
  )

  private val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, existingOfficer))))

  "LiaisonOfficerCommunicationController.onPageLoad" should {

    "render the page with the liaison officer name and communication options in normal mode" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerCommunicationEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                            shouldBe OK
        doc.title()                               shouldBe "How should we communicate with Joe Bloggs? - Manage ISAs - GOV.UK"
        doc.select("h1").text()                   shouldBe "How should we communicate with Joe Bloggs?"
        doc.text()                                  should include("Select all that apply")
        doc.text()                                  should include("By email")
        doc.text()                                  should include("By phone")
        doc.text()                                  should include("By post")
        doc.text()                                  should not include "Liaison officers"
        doc.select("form").attr("action")         shouldBe liaisonOfficerCommunicationEndpointFor(existingId)
        doc.select("form").attr("autocomplete")   shouldBe "off"
        doc.select("button").text()               shouldBe "Continue"
        doc.select("input[type=checkbox]").size() shouldBe 3
      }
    }

    "use the check-mode form action when arriving from check your answers" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result =
          route(application, FakeRequest(GET, changeLiaisonOfficerCommunicationEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                    shouldBe OK
        doc.select("form").attr("action") shouldBe changeLiaisonOfficerCommunicationEndpointFor(existingId)
      }
    }

    "repopulate the saved communication options for the matching liaison officer id" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result         = route(application, FakeRequest(GET, liaisonOfficerCommunicationEndpointFor(existingId))).value
        val doc            = Jsoup.parse(contentAsString(result))
        val selectedValues = doc.select("input[type=checkbox][checked]").eachAttr("value")

        status(result) shouldBe OK
        selectedValues   should contain theSameElementsAs Seq("byEmail", "byPhone")
      }
    }

    "render all options unselected when no communication options have been saved" in {
      val answersWithoutCommunication = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(existingOfficer.copy(communication = Set.empty))))
      )
      val application                 = applicationBuilder(effectiveAnswers = answersWithoutCommunication).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerCommunicationEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                     shouldBe OK
        doc.select("input[type=checkbox][checked]").size() shouldBe 0
        doc.select("input[type=checkbox]").eachAttr("value") should contain theSameElementsAs
          Seq("byEmail", "byPhone", "byPost")
      }
    }

    "redirect to change of circumstances when the identified liaison officer does not exist" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerCommunicationEndpointFor(existingId))).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "redirect to change of circumstances when the identified liaison officer has no phone number" in {
      val officerWithoutPhoneNumber = existingOfficer.copy(phoneNumber = None)
      val application               = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officerWithoutPhoneNumber))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerCommunicationEndpointFor(existingId))).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }

  "LiaisonOfficerCommunicationController.onSubmit" should {

    "save selected communication options in the logged-in session while preserving existing answers" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      val application     = applicationBuilder(
        effectiveAnswers = answers,
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerCommunicationEndpointFor(existingId))
          .withFormUrlEncodedBody("value[0]" -> "byEmail", "value[1]" -> "byPost")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, existingOfficer.copy(communication = Set(ByEmail, ByPost))))
          )
        )
      }
    }

    "save all supported communication options" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerCommunicationEndpointFor(existingId))
          .withFormUrlEncodedBody(
            "value[0]" -> "byEmail",
            "value[1]" -> "byPhone",
            "value[2]" -> "byPost"
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates.liaisonOfficers shouldBe Assign(
          LiaisonOfficers(Seq(otherOfficer, existingOfficer.copy(communication = Set(ByEmail, ByPhone, ByPost))))
        )
      }
    }

    "save and use the check-mode fallback when submitted from check your answers" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, changeLiaisonOfficerCommunicationEndpointFor(existingId))
          .withFormUrlEncodedBody("value[0]" -> "byPhone")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "return the required inline error and not save when no option is selected" in {
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerCommunicationEndpointFor(existingId))
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))
        val error   = "Select the ways you want HMRC to communicate with this liaison officer"

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-summary").text()           should include(error)
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.select(".govuk-error-message").text()           should include(error)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect without saving when the guard fails" in {
      val application = applicationBuilder(effectiveAnswers = Answers()).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerCommunicationEndpointFor(existingId))
          .withFormUrlEncodedBody("value[0]" -> "byEmail")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
