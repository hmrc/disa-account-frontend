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

import play.api.data.Form
import play.api.mvc.Call
import play.api.test.FakeRequest
import org.mockito.Mockito.*
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.Yes
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers, YesNoAnswer}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
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

        val view = contentAsString(result)

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

        val view = contentAsString(result)

        status(result) shouldBe SEE_OTHER
      }
    }
  }
  "RemoveSignatoryController.onSubmit" should {

    "should remove signatories" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories))
      ).build()
      running(application) {
        val result = route(
          application,
          FakeRequest(POST, s"$removeSignatoryEndpoint?id=$testSignatoryId").withFormUrlEncodedBody("value" -> Yes.toString)
        ).value

        status(result) shouldBe BAD_REQUEST
      }
    }

  }
}
