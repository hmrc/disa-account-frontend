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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class FcaArticlesControllerSpec extends BaseUnitSpec {
  private def checkboxIsChecked(html: String, product: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$product]").hasAttr("checked")

  "FcaArticlesController.onPageLoad" should {
    "render an empty page when Financial Articles were newly added in the session" in {
      val answers     = UserAnswers(
        testSessionId
      )
      val application = applicationBuilder(sessionAnswers = Some(answers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, fcaArticlesEndpoint)).value
        val html   = contentAsString(result)

        status(result) shouldBe OK
        FcaArticles.values.foreach { product =>
          checkboxIsChecked(html, product.toString) shouldBe false
        }
      }
    }
  }
  "FcaArticlesController.onSubmit"   should {
    "return Bad Request with the exact inline error when no product is selected" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, fcaArticlesEndpoint).withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val html    = contentAsString(result)
        val doc     = Jsoup.parse(html)

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text()           should include(
          "You need to select which of the Articles apply to your organisation. Select from the options"
        )
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.title()                                         should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }

    }
  }
}
