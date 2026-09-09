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
import org.mockito.Mockito.{never, verify, when}
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.CrowdFundedDebentures
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.*
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class ChangeProductsControllerSpec extends BaseUnitSpec {

  private def signatoryApplicationBuilder(
    effectiveAnswers: Answers,
    originalAnswers: Option[Answers] = None,
    sessionAnswers: Option[UserAnswers] = None
  ) =
    applicationBuilder(
      effectiveAnswers = effectiveAnswers.copy(signatories = Some(testSignatories)),
      originalAnswers = Some(originalAnswers.getOrElse(effectiveAnswers).copy(signatories = Some(testSignatories))),
      sessionAnswers = sessionAnswers,
      email = Some(testSignatoryEmail)
    )

  private def checkboxIsChecked(html: String, product: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$product]").hasAttr("checked")

  "ChangeProductsController.onPageLoad" should {

    "render the content and prefill products from effective answers" in {
      val products    = Seq(StocksAndSharesIsas, InnovativeFinanceIsas)
      val application = signatoryApplicationBuilder(Answers(isaProducts = Some(products))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeProductsEndpoint)).value
        val html   = contentAsString(result)
        val doc    = Jsoup.parse(html)

        status(result)                                                  shouldBe OK
        doc.title()                                                       should startWith("Change ISA products")
        doc.select("h1").text()                                        shouldBe "Change ISA products"
        doc.select("form p.govuk-body").eachText().asScala.toSeq       shouldBe Seq(
          "Any changes to ISA products will require manual processing by HMRC.",
          "Once you have submitted your ISA product changes, you will be unable to update or change your:",
          "Once the changes to the ISA products have been checked and approved you will be able to carry out other updates as required."
        )
        doc.select("ul.govuk-list--bullet li").eachText().asScala.toSeq shouldBe Seq(
          "organisation details",
          "further ISA products amendments",
          "authorised users"
        )
        doc.select("legend").text()                                    shouldBe
          "Which ISA products does your organisation offer?"
        doc.select(".govuk-hint").text()                               shouldBe
          "Select or remove the ISA products your organisation offers"
        doc.select(".govuk-checkboxes__label").eachText().asScala.toSeq shouldBe Seq(
          "Cash ISAs",
          "Cash Junior ISAs",
          "Stocks and Shares ISAs",
          "Stocks and Shares Junior ISAs",
          "Innovative Finance ISAs"
        )
        checkboxIsChecked(html, CashIsas.toString)                      shouldBe false
        checkboxIsChecked(html, StocksAndSharesIsas.toString)           shouldBe true
        checkboxIsChecked(html, InnovativeFinanceIsas.toString)         shouldBe true
        doc.select("button.govuk-button").text()                        shouldBe "Continue"
      }
    }

    "render every product unchecked when there are no effective product answers" in {
      val application = signatoryApplicationBuilder(Answers()).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeProductsEndpoint)).value
        val html   = contentAsString(result)

        status(result) shouldBe OK
        IsaProduct.values.foreach(product => checkboxIsChecked(html, product.toString) shouldBe false)
      }
    }

    "redirect a non-signatory to change of circumstances" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories)),
        email = Some("not-a-signatory@example.com")
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeProductsEndpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
      }
    }
  }

  "ChangeProductsController.onSubmit" should {

    "show the required error when no product is selected" in {
      val application = signatoryApplicationBuilder(Answers(isaProducts = Some(Seq(CashIsas)))).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text()           should include(
          "Select the ISA products your organisation offers"
        )
        doc.select(".govuk-error-summary a").text()       shouldBe
          "Select the ISA products your organisation offers"
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.title()                                         should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "reject an unsupported product without saving" in {
      val application = signatoryApplicationBuilder(Answers(isaProducts = Some(Seq(CashIsas)))).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody("value[0]" -> "unsupported")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "save an ordinary product change in display order and preserve unrelated updates" in {
      val answers         = Answers(isaProducts = Some(Seq(CashIsas)))
      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        answers,
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> StocksAndSharesIsas.toString,
            "value[]" -> CashIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          isaProducts = Assign(Seq(CashIsas, StocksAndSharesIsas))
        )
      }
    }

    "save selections, preserve unrelated updates and clear dependent answers when Innovative Finance is removed" in {
      val originalAnswers = Answers(
        isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Some(Seq(CrowdFundedDebentures))
      )
      val effectiveAnswers = originalAnswers
      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = signatoryApplicationBuilder(
        effectiveAnswers,
        Some(originalAnswers),
        Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> StocksAndSharesIsas.toString,
            "value[]" -> CashIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          isaProducts = Assign(Seq(CashIsas, StocksAndSharesIsas)),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
      }
    }

    "restore ETMP dependent answers and return to change of circumstances when Innovative Finance is reticked" in {
      val originalAnswers = Answers(
        isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Some(Seq(CrowdFundedDebentures))
      )
      val effectiveAnswers = Answers(isaProducts = Some(Seq(CashIsas)))
      val existingUpdates = SessionUpdates(
        isaProducts = Assign(Seq(CashIsas)),
        innovativeFinancialProducts = Clear,
        p2pPlatform = Clear,
        p2pPlatformNumber = Clear
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = signatoryApplicationBuilder(
        effectiveAnswers,
        Some(originalAnswers),
        Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> CashIsas.toString,
            "value[]" -> InnovativeFinanceIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
          innovativeFinancialProducts = Unchanged,
          p2pPlatform = Unchanged,
          p2pPlatformNumber = Unchanged
        )
      }
    }

    "clear dependent answers and enter the Innovative Finance journey when the product is newly selected" in {
      val answers = Answers(isaProducts = Some(Seq(CashIsas)))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = signatoryApplicationBuilder(answers).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> CashIsas.toString,
            "value[]" -> InnovativeFinanceIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(innovativeFinancialProductsEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe SessionUpdates(
          isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
      }
    }

    "return to the Innovative Finance journey when the product is selected but its question is unanswered" in {
      val originalAnswers  = Answers(isaProducts = Some(Seq(CashIsas)))
      val effectiveAnswers = Answers(isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)))
      val existingUpdates  = SessionUpdates(
        isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Clear,
        p2pPlatform = Clear,
        p2pPlatformNumber = Clear
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application      = signatoryApplicationBuilder(
        effectiveAnswers,
        Some(originalAnswers),
        Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> CashIsas.toString,
            "value[]" -> InnovativeFinanceIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(innovativeFinancialProductsEndpoint)
      }
    }

    "return to change of circumstances when the Innovative Finance question is answered" in {
      val originalAnswers  = Answers(isaProducts = Some(Seq(CashIsas)))
      val effectiveAnswers = Answers(
        isaProducts = Some(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Some(Seq(CrowdFundedDebentures))
      )
      val existingUpdates  = SessionUpdates(
        isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Assign(Seq(CrowdFundedDebentures))
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application      = signatoryApplicationBuilder(
        effectiveAnswers,
        Some(originalAnswers),
        Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> CashIsas.toString,
            "value[]" -> InnovativeFinanceIsas.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
      }
    }

    "redirect a non-signatory without saving" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(testSignatories)),
        email = Some("not-a-signatory@example.com")
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeProductsEndpoint)
          .withFormUrlEncodedBody("value[]" -> CashIsas.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
