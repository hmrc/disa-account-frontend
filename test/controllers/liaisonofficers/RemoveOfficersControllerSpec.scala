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
import org.mockito.Mockito.*
import play.api.data.Form
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.{No, Yes}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers, YesNoAnswer}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.ByPost
import utils.BaseUnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign

class RemoveOfficersControllerSpec extends BaseUnitSpec {

  def onwardRoute(path: String): Call = Call("GET", s"/obligations/enrolment/isa$path")

  val formProvider: YesNoAnswerFormProvider = new YesNoAnswerFormProvider()
  val form: Form[YesNoAnswer]               = formProvider("removeLiaisonOfficer.error.required")

  private val otherOfficer: LiaisonOfficer =
    LiaisonOfficer(
      id = "officer-2",
      fullName = Some("Other Person"),
      email = Some("officer2@test.com"),
      phoneNumber = Some("07777777777"),
      communication = Set(ByPost)
    )

  "RemoveOfficersController.onPageLoad" should {
    "must return OK and the correct view for a GET when the officer exists" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(testLiaisonOfficers))
      ).build()

      running(application) {

        val result =
          route(application, FakeRequest(GET, s"$removeLiaisonOfficerEndpoint?id=$testLiaisonOfficerId")).value

        status(result)        shouldBe OK
        contentAsString(result) should include(testName)
      }
    }

    "must return 303 error when trying to access an inexistent id" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(testLiaisonOfficers))
      ).build()

      running(application) {

        val result = route(application, FakeRequest(GET, s"$removeLiaisonOfficerEndpoint?id=officer-12345")).value

        status(result) shouldBe SEE_OTHER
      }
    }

  }

  "RemoveOfficersController.onSubmit" should {
    "remove the selected officer when Yes is submitted" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val officers    = LiaisonOfficers(testLiaisonOfficers.liaisonOfficers :+ otherOfficer)
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(officers))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeLiaisonOfficerEndpoint?id=$testLiaisonOfficerId")
          .withFormUrlEncodedBody("value" -> Yes.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe addedLiaisonOfficerEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id                      shouldBe testSessionId
        captor.getValue.updates.liaisonOfficers shouldBe Assign(
          LiaisonOfficers(Seq(otherOfficer))
        )
      }
    }

    "preserve the officers when No is submitted" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val officers    = LiaisonOfficers(testLiaisonOfficers.liaisonOfficers :+ otherOfficer)
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(officers))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeLiaisonOfficerEndpoint?id=$testLiaisonOfficerId")
          .withFormUrlEncodedBody("value" -> No.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe addedLiaisonOfficerEndpoint

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id                      shouldBe testSessionId
        captor.getValue.updates.liaisonOfficers shouldBe Assign(officers)
      }
    }

    "return an error and not save when no answer is submitted" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(testLiaisonOfficers))
      ).build()

      running(application) {
        val request = FakeRequest(POST, s"$removeLiaisonOfficerEndpoint?id=$testLiaisonOfficerId")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(
          "Select yes if you want to remove this liaison officer"
        )
        doc.title()                               should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }

}
