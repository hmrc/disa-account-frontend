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
import play.api.inject.bind
import play.api.test.Helpers.*
import play.api.test.FakeRequest
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.utils.UuidGenerator
import utils.BaseUnitSpec

import scala.concurrent.Future

class LiaisonOfficerNameControllerSpec extends BaseUnitSpec {

  private val existingId = "existing-id"

  private val existingOfficer = LiaisonOfficer(
    id = existingId,
    fullName = Some("Old Name"),
    phoneNumber = Some("07777777777"),
    communication = Set(ByEmail, ByPhone),
    email = Some("old.name@example.com")
  )

  "LiaisonOfficerNameController.onPageLoad" should {

    "generate an id and redirect to the canonical URL when no id is supplied" in {
      val generatedId   = "generated-id"
      val uuidGenerator = mock[UuidGenerator]
      when(uuidGenerator.generate()).thenReturn(generatedId)

      val application = applicationBuilder()
        .overrides(bind[UuidGenerator].toInstance(uuidGenerator))
        .build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerNameEndpoint)).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe liaisonOfficerNameEndpointFor(generatedId)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "render the empty identified page with the supplied content" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerNameEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        doc.title() shouldBe
          "What is the full name of the liaison officer? - Liaison officers - Manage ISAs - GOV.UK"
        doc.text() should include("What is the full name of the liaison officer?")
        doc.text() should include(
          "You must have at least 1 liaison officer to register your organisation as an ISA manager. " +
            "You can add up to 15 liaison officers."
        )
        doc.select(".govuk-caption-l").text() shouldBe "This section is Liaison officers"
        doc.select("button").text() shouldBe "Continue"
      }
    }

    "repopulate the name for the matching liaison officer id" in {
      val otherOfficer = LiaisonOfficer("other-id", Some("Other Name"))
      val application  = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, existingOfficer))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, liaisonOfficerNameEndpointFor(existingId))).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        doc.select("input[name=value]").attr("value") shouldBe "Old Name"
      }
    }
  }

  "LiaisonOfficerNameController.onSubmit" should {

    "trim and save the identified officer while preserving other answers and officer details" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val otherOfficer    = LiaisonOfficer("other-id", Some("Other Name"))
      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      val application     = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, existingOfficer)))),
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerNameEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "  Updated Name  ")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, existingOfficer.copy(fullName = Some("Updated Name"))))
          )
        )
      }
    }

    "save a new officer when the generated id is not already present" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val newId       = "new-id"
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerNameEndpointFor(newId))
          .withFormUrlEncodedBody("value" -> "New Name")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates.liaisonOfficers shouldBe Assign(
          LiaisonOfficers(Seq(LiaisonOfficer(newId, Some("New Name"))))
        )
      }
    }

    "return BadRequest with the required error when the field is blank" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerNameEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "   ")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include("Enter the full name of the liaison officer you’re adding")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return BadRequest with the invalid-character error when the name is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, liaisonOfficerNameEndpointFor(existingId))
          .withFormUrlEncodedBody("value" -> "Jane Smith 2")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        contentAsString(result) should include(
          "The full name must only include letters a to z, hyphens, spaces and apostrophes"
        )
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
