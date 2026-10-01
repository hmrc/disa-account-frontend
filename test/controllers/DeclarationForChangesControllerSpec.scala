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

import controllers.actions.FakeAccountMaintenanceGuardAction
import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, StocksAndSharesIsas}
import uk.gov.hmrc.disaaccountfrontend.models.registration.UpdateRegistrationDetailsRequest
import utils.BaseUnitSpec

import scala.concurrent.Future

class DeclarationForChangesControllerSpec extends BaseUnitSpec {

  "DeclarationForChangesController.onPageLoad" should {

    "redirect to manage ISAs when an ISA product change is under review" in {
      val application = applicationBuilder(
        accountMaintenanceGuard = new FakeAccountMaintenanceGuardAction(blocked = true)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, declarationForChangesEndpoint)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe manageIsasEndpoint
      }
    }
    "use the ISA products title when ISA products changed" in {
      val originalAnswers  = Answers(isaProducts = Some(Seq(CashIsas)))
      val effectiveAnswers = Answers(isaProducts = Some(Seq(CashIsas, StocksAndSharesIsas)))
      val application      = applicationBuilder(
        effectiveAnswers = effectiveAnswers,
        originalAnswers = Some(originalAnswers)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, declarationForChangesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                  shouldBe OK
        doc.select("h1").text()                         shouldBe "Declaration for changes, including ISA products"
        doc.select("title").text()                        should startWith("Declaration for changes, including ISA products")
        doc.select("h1 + p").text()                     shouldBe
          "Before you submit your changes, you must agree to the following statements:"
        doc.select("ul.govuk-list--bullet li").eachText() should contain theSameElementsInOrderAs Seq(
          "the ISA products and investments will meet the qualifying criteria as described in the ISA regulations and ISA managers’ guidance",
          "the ISAs opened and subscriptions made under my management will comply with the ISA regulations",
          "all HMRC ISA returns and reporting obligations will be accurate and made in accordance with the ISA regulations and ISA managers’ guidance",
          "I’m not subject to any requirement or prohibition imposed by or under any rules made by the Financial Conduct Authority or the Prudential Regulation Authority under Part 4A of FISMA 2000 which would prevent me from acting as an account manager",
          "the information I’ve provided in this change request is, to the best of my knowledge and belief, correct and true",
          "I understand that if my circumstances change I’ll notify HM Revenue and Customs immediately"
        )
        doc.select("ul + p").text()                     shouldBe
          "By continuing, I confirm that I understand and agree to these statements."
        doc.select("form").attr("method")               shouldBe "POST"
        doc.select("form").attr("action")                 should endWith(declarationForChangesEndpoint)
        doc.select("button").text()                     shouldBe "I agree - submit"
      }
    }

    "use the standard title when ISA products did not change" in {
      val answers     = Answers(isaProducts = Some(Seq(CashIsas)))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, declarationForChangesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                  shouldBe OK
        doc.select("h1").text()                         shouldBe "Declaration for changes"
        doc.select("title").text()                        should startWith("Declaration for changes -")
        doc.select("h1 + p").text()                     shouldBe "Do you agree to the following statements:"
        doc.select("ul.govuk-list--bullet li").eachText() should contain theSameElementsInOrderAs Seq(
          "the ISA products and investments will meet the qualifying criteria as described in the ISA regulations and ISA managers’ guidance",
          "the ISAs opened and subscriptions made under my management will comply with the ISA regulations",
          "I’m not subject to any requirement or prohibition imposed by or under any rules made by the Financial Conduct Authority or the Prudential Regulation Authority under Part 4A of FISMA 2000 which would prevent me from acting as an account manager",
          "the information I’ve provided in this change request is, to the best of my knowledge and belief, correct and true"
        )
        doc.select("ul + p").text()                     shouldBe
          "By continuing, I confirm that I understand and agree to these statements."
      }
    }
  }

  "DeclarationForChangesController.onSubmit" should {

    "redirect to manage ISAs when an ISA product change is under review" in {
      val application = applicationBuilder(
        accountMaintenanceGuard = new FakeAccountMaintenanceGuardAction(blocked = true)
      ).build()

      running(application) {
        val request = FakeRequest(POST, declarationForChangesEndpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe manageIsasEndpoint
      }
    }

    "send the effective answers and redirect to changes completed" in {
      val effectiveAnswers = Answers(tradingName = Some("Updated name"))
      when(mockRegistrationConnector.updateRegistrationDetails(any[String], any[UpdateRegistrationDetailsRequest])(any))
        .thenReturn(Future.successful(()))
      val application      = applicationBuilder(effectiveAnswers = effectiveAnswers).build()

      running(application) {
        val request = FakeRequest(POST, declarationForChangesEndpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changesCompletedEndpoint)
        verify(mockRegistrationConnector)
          .updateRegistrationDetails(eqTo(testZref), eqTo(UpdateRegistrationDetailsRequest(effectiveAnswers)))(any)
      }
    }

    "propagate the failure instead of redirecting when the update fails" in {
      val exception   = new RuntimeException("boom")
      when(mockRegistrationConnector.updateRegistrationDetails(any[String], any[UpdateRegistrationDetailsRequest])(any))
        .thenReturn(Future.failed(exception))
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, declarationForChangesEndpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        result.failed.futureValue shouldBe exception
      }
    }
  }
}
