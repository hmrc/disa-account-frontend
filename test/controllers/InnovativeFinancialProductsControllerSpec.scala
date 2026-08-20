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

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.*
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class InnovativeFinancialProductsControllerSpec extends BaseUnitSpec {

  val endpoint: String = "/obligations/account/isa/innovative-financial-products"

  private val enrolledEffectiveAnswers = SessionUpdates(
    isaProducts = Some(testIsaProductSelections),
    innovativeFinancialProducts = Some(testInnovativeFinancialProductSelections)
  )

  private def checkboxIsChecked(html: String, product: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$product]").hasAttr("checked")

  "InnovativeFinancialProductsController.onPageLoad" should {

    "render and prefill the page from effective answers when Innovative Finance ISAs are already offered" in {
      val application = applicationBuilder(
        effectiveAnswers = enrolledEffectiveAnswers
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, endpoint)).value
        val html   = contentAsString(result)

        status(result)                                                         shouldBe OK
        html                                                                     should include("Which types of innovative finance products will your organisation offer?")
        checkboxIsChecked(html, PeertopeerLoansAndHave36hPermissions.toString) shouldBe true
        checkboxIsChecked(html, CrowdFundedDebentures.toString)                shouldBe true
      }
    }

    "render an empty page when Innovative Finance ISAs were newly added in the session" in {
      val answers     = UserAnswers(
        testSessionId,
        SessionUpdates(isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)))
      )
      val application = applicationBuilder(
        effectiveAnswers = answers.updates,
        sessionAnswers = Some(answers)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, endpoint)).value
        val html   = contentAsString(result)

        status(result) shouldBe OK
        InnovativeFinancialProduct.values.foreach { product =>
          checkboxIsChecked(html, product.toString) shouldBe false
        }
      }
    }

    "replay the innovative finance answers supplied by the retrieval action" in {
      val answers     = UserAnswers(
        testSessionId,
        SessionUpdates(
          isaProducts = Some(Seq(InnovativeFinanceIsas)),
          innovativeFinancialProducts = Some(Seq(LongTermAssetFunds))
        )
      )
      val application = applicationBuilder(
        effectiveAnswers = answers.updates,
        sessionAnswers = Some(answers)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, endpoint)).value
        val html   = contentAsString(result)

        status(result)                                          shouldBe OK
        checkboxIsChecked(html, LongTermAssetFunds.toString)    shouldBe true
        checkboxIsChecked(html, CrowdFundedDebentures.toString) shouldBe false
      }
    }

    "redirect when Innovative Finance ISAs are absent from both session and enrolment" in {
      val application = applicationBuilder(
        effectiveAnswers = SessionUpdates(isaProducts = Some(Seq(CashIsas)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, endpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith("/change-of-circumstances")
      }
    }

    "redirect when the session deselects Innovative Finance ISAs from an existing enrolment" in {
      val answers     = UserAnswers(testSessionId, SessionUpdates(isaProducts = Some(Seq(CashIsas))))
      val application = applicationBuilder(
        effectiveAnswers = answers.updates,
        sessionAnswers = Some(answers)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, endpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith("/change-of-circumstances")
      }
    }
  }

  "InnovativeFinancialProductsController.onSubmit" should {

    "save products in display order, preserve other session answers and redirect" in {
      val existingUpdates = SessionUpdates(
        correspondenceAddress = Some(testCorrespondenceAddress),
        organisationTelephoneNumber = Some(testOrgTelephoneNumber),
        isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas))
      )
      val existingAnswers = UserAnswers(testSessionId, existingUpdates)
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = applicationBuilder(
        effectiveAnswers = existingUpdates,
        sessionAnswers = Some(existingAnswers)
      ).build()

      running(application) {
        val request = FakeRequest(POST, endpoint)
          .withFormUrlEncodedBody(
            "value[3]" -> LongTermAssetFunds.toString,
            "value[0]" -> PeertopeerLoansAndHave36hPermissions.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith("/change-of-circumstances")

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates.correspondenceAddress       shouldBe Some(testCorrespondenceAddress)
        captor.getValue.updates.organisationTelephoneNumber shouldBe Some(testOrgTelephoneNumber)
        captor.getValue.updates.isaProducts                 shouldBe Some(Seq(CashIsas, InnovativeFinanceIsas))
        captor.getValue.updates.innovativeFinancialProducts shouldBe Some(
          Seq(PeertopeerLoansAndHave36hPermissions, LongTermAssetFunds)
        )
      }
    }

    "use the temporary change-of-circumstances fallback when the platform-with-36H option is selected" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder(
        effectiveAnswers = enrolledEffectiveAnswers
      ).build()

      running(application) {
        val request = FakeRequest(POST, endpoint)
          .withFormUrlEncodedBody(
            "value[1]" -> PeertopeerLoansUsingAPlatformWith36hPermissions.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith("/change-of-circumstances")
        verify(mockUserAnswersRepository).set(any())
      }
    }

    "return Bad Request with the exact inline error when no product is selected" in {
      val application = applicationBuilder(
        effectiveAnswers = enrolledEffectiveAnswers
      ).build()

      running(application) {
        val request = FakeRequest(POST, endpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val html    = contentAsString(result)
        val doc     = Jsoup.parse(html)

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text()           should include(
          "Select which types of innovative finance products your organisation will offer"
        )
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.title()                                         should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect without saving when the session has deselected Innovative Finance ISAs" in {
      val answers     = UserAnswers(testSessionId, SessionUpdates(isaProducts = Some(Seq(CashIsas))))
      val application = applicationBuilder(
        effectiveAnswers = answers.updates,
        sessionAnswers = Some(answers)
      ).build()

      running(application) {
        val request = FakeRequest(POST, endpoint)
          .withFormUrlEncodedBody("value[0]" -> CrowdFundedDebentures.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith("/change-of-circumstances")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
