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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.{No, Yes}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers, YesNoAnswer}
import utils.BaseUnitSpec

import scala.concurrent.Future

class RemoveSignatoryControllerSpec extends BaseUnitSpec {

  def onwardRoute(path: String): Call = Call("GET", s"/obligations/enrolment/isa$path")

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[YesNoAnswer]               = formProvider("removeSignatory.error.required")

  private val otherSignatory =
    Signatory(
      id = "signatory-2",
      fullName = Some("Other Person"),
      jobTitle = Some("Job title")
    )

  "RemoveSignatoryController.onPageLoad" should {

    "must return OK and the correct view for a GET when the signatory exists" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {

        val result = route(application, FakeRequest(GET, s"$removeSignatoryEndpoint?id=$testSignatoryId")).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testSignatoryName)
      }
    }

    "must return 303 error when trying to access an inexistent id" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {

        val result = route(application, FakeRequest(GET, s"$removeSignatoryEndpoint?id=signatory-12345")).value

        status(result) shouldBe SEE_OTHER
      }
    }
  }
  "RemoveSignatoryController.onSubmit" should {

    "remove the selected signatory when Yes is submitted" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val signatories = Signatories(testSignatories.signatories :+ otherSignatory)
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(signatories))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeSignatoryEndpoint?id=$testSignatoryId")
          .withFormUrlEncodedBody("value" -> Yes.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id                  shouldBe testSessionId
        captor.getValue.updates.signatories shouldBe Assign(Signatories(Seq(otherSignatory)))
      }
    }

    "preserve the signatories when No is submitted" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val signatories = Signatories(testSignatories.signatories :+ otherSignatory)
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(signatories))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeSignatoryEndpoint?id=$testSignatoryId")
          .withFormUrlEncodedBody("value" -> No.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id                  shouldBe testSessionId
        captor.getValue.updates.signatories shouldBe Assign(signatories)
      }
    }

    "return an error and not save when no answer is submitted" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeSignatoryEndpoint?id=$testSignatoryId")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(
          "Select yes if you want to remove this signatory"
        )
        doc.title()                               should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
