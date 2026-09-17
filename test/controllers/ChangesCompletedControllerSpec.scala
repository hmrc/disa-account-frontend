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
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, StocksAndSharesIsas}
import utils.BaseUnitSpec

class ChangesCompletedControllerSpec extends BaseUnitSpec {

  "ChangesCompletedController.onPageLoad" should {
    "render the page and ISA product section when products changed" in {
      val originalAnswers  = Answers(isaProducts = Some(Seq(CashIsas)))
      val effectiveAnswers = Answers(isaProducts = Some(Seq(CashIsas, StocksAndSharesIsas)))
      val application      = applicationBuilder(
        effectiveAnswers = effectiveAnswers,
        originalAnswers = Some(originalAnswers)
      ).build()

      running(application) {
        val result       = route(application, FakeRequest(GET, changesCompletedEndpoint)).value
        val doc          = Jsoup.parse(contentAsString(result))
        val guidanceLink = doc.select(
          "a[href=https://www.gov.uk/government/collections/isa-managers-guidance]"
        )

        status(result)                                             shouldBe OK
        doc.select(".govuk-panel__title").text()                   shouldBe "Changes completed"
        doc.select("main").text()                                    should include("You have completed your changes.")
        doc.select("main").text()                                    should include(
          "The changes you have made will appear on your account. If you need to manage further changes you can from the manage ISAs start page."
        )
        doc.select("main").text()                                    should include(
          "If you have updated any ISA products using the service, it will now need to be manually checked by HMRC."
        )
        doc.select("main").text()                                    should include(
          "You will not be able to make any further changes until this is done and you receive a letter from HMRC."
        )
        doc.select("h2:matchesOwn(^What happens next$)").size()    shouldBe 1
        doc.select("h2:matchesOwn(^Updated ISA products$)").size() shouldBe 1
        doc.select("a[href=/]").size()                             shouldBe 2
        doc.select("a[href=/]").last().text()                      shouldBe "Go back to manage ISAs start page"
        guidanceLink.attr("target")                                shouldBe "_blank"
        guidanceLink.text()                                        shouldBe
          "read the guidance in the ISA managers guidance collection (opens in new tab)"
      }
    }

    "hide the ISA product section when products did not change" in {
      val answers     = Answers(isaProducts = Some(Seq(CashIsas)))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changesCompletedEndpoint)).value

        status(result)        shouldBe OK
        contentAsString(result) should not include "Updated ISA products"
      }
    }
  }
}
