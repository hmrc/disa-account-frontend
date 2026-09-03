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
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import utils.BaseUnitSpec

class SignatoryCheckYourAnswersControllerSpec extends BaseUnitSpec {

  private val signatory = Signatory(testSignatoryId, Some(testSignatoryName), Some(testSignatoryJobTitle))
  private val url       = s"$checkSignatoryDetailsEndpoint?id=$testSignatoryId"

  "SignatoryCheckYourAnswersController.onPageLoad" should {

    "render the matching signatory details and change links" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(signatory)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                         shouldBe OK
        doc.title()                            shouldBe "Check signatory details - Manage ISAs - GOV.UK"
        doc.select("h1").text()                shouldBe "Check signatory details"
        doc.text()                               should include(testSignatoryName)
        doc.text()                               should include(testSignatoryJobTitle)
        doc.text()                               should include("Name")
        doc.text()                               should include("Job title within organisation")
        doc.select(".govuk-caption-l").isEmpty shouldBe true

        val links = doc.select(".govuk-summary-list__actions a")
        links.get(0).attr("href") shouldBe s"$changeSignatoryNameEndpoint?id=$testSignatoryId"
        links.get(0).text()       shouldBe "Change name of signatory"
        links.get(1).attr("href") shouldBe s"$changeSignatoryJobTitleEndpoint?id=$testSignatoryId"
        links.get(1).text()       shouldBe "Change job title"

        val continue = doc.select("a.govuk-button")
        continue.text()       shouldBe "Continue"
        continue.attr("href") shouldBe addedSignatoriesEndpoint
        doc.text()              should include("Is this page not working properly?")
      }
    }

    "redirect when the signatory cannot be found" in {
      val application = applicationBuilder(effectiveAnswers = Answers(signatories = Some(Seq.empty))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "redirect when the signatory details are incomplete" in {
      val incomplete  = signatory.copy(jobTitle = None)
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(incomplete)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }
  }
}
