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
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, StocksAndSharesIsas}
import utils.BaseUnitSpec

class ChangesCompletedControllerSpec extends BaseUnitSpec {

  "ChangesCompletedController.onPageLoad" should {

    "render the ISA product review and confirmation content when products changed" in {
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

        status(result)                                          shouldBe OK
        doc.select(".govuk-panel__title").text()                shouldBe "Changes completed"
        doc.select("main").text()                                 should include("You have completed your changes.")
        doc.select("h2:matchesOwn(^What happens next$)").size() shouldBe 1
        doc.select("main").text()                                 should include(
          "You have made changes to your ISA products. This means:"
        )
        doc.select("ul.govuk-list--bullet li").eachText()         should contain theSameElementsInOrderAs Seq(
          "HMRC will manually check added or removed ISA products",
          "you will not be able to make any further changes until these checks have been completed"
        )
        doc.select("h2:matchesOwn(^Confirmation$)").size()      shouldBe 1
        doc.select("main").text()                                 should include(
          "HMRC will email you once the necessary checks have been made to the ISA products you’ve updated. You will then be able to make further amendments to your account using this service."
        )
        doc.select("main").text()                                 should include(
          "The changes to your ISA products will be confirmed by HMRC within 2 weeks of you declaring them, by email."
        )
        val manageIsasLinks = doc.select(s"a[href=$manageIsasEndpoint]")
        manageIsasLinks.size() shouldBe 1
        manageIsasLinks.text() shouldBe "Go back to manage ISAs start page"
        guidanceLink.text()      should include("ISA managers’ guidance collection")
      }
    }

    "render the general completion content when products did not change" in {
      val answers     = Answers(isaProducts = Some(Seq(CashIsas)))
      val application = applicationBuilder(effectiveAnswers = answers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changesCompletedEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                     shouldBe OK
        doc.select("main").text()                            should include(
          "The changes you have made will appear on your account. If you need to manage further changes you can from the manage ISAs start page."
        )
        doc.select("h2:matchesOwn(^Confirmation$)").size() shouldBe 0
        doc.select("ul.govuk-list--bullet li").size()      shouldBe 0
      }
    }
  }
}
