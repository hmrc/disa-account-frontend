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

package controllers.signatories

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class SignatoryNameControllerSpec extends BaseUnitSpec {

  val validFormData: Map[String, String] = Map("value" -> testSignatoryName)

  val maxSignatories: Seq[Signatory] =
    (1 to 25).map(i => Signatory(s"signatory-$i", fullName = Some(s"Signatory $i")))

  "SignatoryNameController.onPageLoad" should {

    "redirect to itself with a newly generated id when no id is supplied" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, signatoryNameEndpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should startWith(s"$signatoryNameEndpoint?id=")
      }
    }

    "return 200 OK prefilled from the effective answers when the signatory already has a name" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryNameEndpoint?id=$testSignatoryId")).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testSignatoryName)
      }
    }

    "render the check-mode form for an existing signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(GET, s"$changeSignatoryNameEndpoint?id=$testSignatoryId")
        ).value

        status(result)        shouldBe OK
        contentAsString(result) should include(s"action=\"$changeSignatoryNameEndpoint?id=$testSignatoryId\"")
      }
    }

    "reject an unknown signatory id in check mode" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$changeSignatoryNameEndpoint?id=unknown-id")).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "return 200 OK with an empty form when the id does not match an existing signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryNameEndpoint?id=some-other-id")).value

        status(result)        shouldBe OK
        contentAsString(result) should not include testSignatoryName
      }
    }

    "redirect away instead of generating a new id when the maximum number of signatories has been reached" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(maxSignatories))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, signatoryNameEndpoint)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }

  "SignatoryNameController.onSubmit" should {

    "add a new signatory to the existing list when the id does not already exist" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val newId   = "signatory-2"
        val request =
          FakeRequest(POST, s"$signatoryNameEndpoint?id=$newId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          signatories = Assign(testSignatories :+ Signatory(newId, fullName = Some(testSignatoryName)))
        )
      }
    }

    "update the matching signatory's name and preserve their job title" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val existingSignatory = Signatory(testSignatoryId, fullName = Some("Old Name"), jobTitle = Some("Director"))
      val application       = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(existingSignatory)))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryNameEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          signatories = Assign(Seq(existingSignatory.copy(fullName = Some(testSignatoryName))))
        )
      }
    }

    "return directly to check signatory details after updating a name in check mode" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val existingSignatory =
        Signatory(testSignatoryId, fullName = Some("Old Name"), jobTitle = Some(testSignatoryJobTitle))
      val application       = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(existingSignatory)))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$changeSignatoryNameEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe s"$checkSignatoryDetailsEndpoint?id=$testSignatoryId"
      }
    }

    "reject an unknown signatory id in check mode" in {
      val application = applicationBuilder(effectiveAnswers = Answers(signatories = Some(testSignatories))).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$changeSignatoryNameEndpoint?id=unknown-id")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder().build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryNameEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody("value" -> "")
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect away and not save when adding a new signatory would exceed the maximum" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(maxSignatories))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryNameEndpoint?id=a-brand-new-id")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "still allow editing an existing signatory's name when the maximum has been reached" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(maxSignatories))
      ).build()

      running(application) {
        val existingId = maxSignatories.head.id
        val request    =
          FakeRequest(POST, s"$signatoryNameEndpoint?id=$existingId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        verify(mockUserAnswersRepository).set(any())
      }
    }
  }
}
