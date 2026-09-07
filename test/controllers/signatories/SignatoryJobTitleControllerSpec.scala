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

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class SignatoryJobTitleControllerSpec extends BaseUnitSpec {

  val validFormData: Map[String, String] = Map("value" -> testSignatoryJobTitle)

  val existingSignatory: Signatory = Signatory(testSignatoryId, fullName = Some(testSignatoryName))

  "SignatoryJobTitleController.onPageLoad" should {

    "return 200 OK with an empty form when the signatory has no job title yet" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryJobTitleEndpoint?id=$testSignatoryId")).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testSignatoryName)
      }
    }

    "return 200 OK prefilled from the effective answers when the signatory already has a job title" in {
      val application = applicationBuilder(
        effectiveAnswers =
          Answers(signatories = Some(Signatories(Seq(existingSignatory.copy(jobTitle = Some(testSignatoryJobTitle))))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryJobTitleEndpoint?id=$testSignatoryId")).value

        val doc = Jsoup.parse(contentAsString(result))

        status(result)                         shouldBe OK
        contentAsString(result)                  should include(testSignatoryJobTitle)
        doc.title()                            shouldBe s"What is the job title of $testSignatoryName within the organisation? - Manage ISAs - GOV.UK"
        doc.select(".govuk-caption-l").isEmpty shouldBe true
      }
    }

    "render the check-mode form for an existing signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(
          signatories = Some(Signatories(Seq(existingSignatory.copy(jobTitle = Some(testSignatoryJobTitle)))))
        )
      ).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(GET, s"$changeSignatoryJobTitleEndpoint?id=$testSignatoryId")
        ).value

        status(result)        shouldBe OK
        contentAsString(result) should include(s"action=\"$changeSignatoryJobTitleEndpoint?id=$testSignatoryId\"")
      }
    }

    "redirect to change of circumstances when the id does not match an existing signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryJobTitleEndpoint?id=some-other-id")).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "redirect to change of circumstances when the matching signatory has no name yet" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(Signatory(testSignatoryId)))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, s"$signatoryJobTitleEndpoint?id=$testSignatoryId")).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }

  "SignatoryJobTitleController.onSubmit" should {

    "update the matching signatory's job title and preserve their name" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryJobTitleEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe s"$checkSignatoryDetailsEndpoint?id=$testSignatoryId"

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          signatories = Assign(Signatories(Seq(existingSignatory.copy(jobTitle = Some(testSignatoryJobTitle)))))
        )
      }
    }

    "return directly to check signatory details after updating a job title in check mode" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$changeSignatoryJobTitleEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe s"$checkSignatoryDetailsEndpoint?id=$testSignatoryId"
      }
    }

    "return 400 BadRequest when the form is invalid" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryJobTitleEndpoint?id=$testSignatoryId")
            .withFormUrlEncodedBody("value" -> "")
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)        shouldBe BAD_REQUEST
        contentAsString(result) should include(testSignatoryName)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect to change of circumstances and not save when the id does not match an existing signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      ).build()

      running(application) {
        val request =
          FakeRequest(POST, s"$signatoryJobTitleEndpoint?id=some-other-id")
            .withFormUrlEncodedBody(validFormData.toSeq: _*)
            .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
