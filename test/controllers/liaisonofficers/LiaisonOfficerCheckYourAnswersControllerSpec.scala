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

import controllers.actions.FakeAccountMaintenanceGuardAction
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficers
import utils.BaseUnitSpec

class LiaisonOfficerCheckYourAnswersControllerSpec extends BaseUnitSpec {

  private val url = s"$checkLiaisonOfficerDetailsEndpoint?id=$testLiaisonOfficerId"

  "LiaisonOfficerCheckYourAnswersController.onPageLoad" should {

    "redirect to manage ISAs when an ISA product change is under review" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(testLiaisonOfficer)))),
        accountMaintenanceGuard = new FakeAccountMaintenanceGuardAction(blocked = true)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe manageIsasEndpoint
      }
    }

    "render the matching liaison officer details and change links" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(testLiaisonOfficer))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                         shouldBe OK
        doc.title()                            shouldBe "Check liaison officer details - Manage ISAs - GOV.UK"
        doc.select("h1").text()                shouldBe "Check liaison officer details"
        doc.text()                               should include(testName)
        doc.text()                               should include(testEmail)
        doc.text()                               should include("Name")
        doc.text()                               should include("Communication preferences")
        doc.select(".govuk-caption-l").isEmpty shouldBe true

        val links = doc.select(".govuk-summary-list__actions a")
        links.get(0).attr("href") shouldBe s"$changeLiaisonOfficerNameEndpoint?id=$testLiaisonOfficerId"
        links.get(0).text()       shouldBe "Change name of liaison officer"
        links.get(2).attr("href") shouldBe s"$changeLiaisonOfficerPhoneNumberEndpoint?id=$testLiaisonOfficerId"
        links.get(2).text()       shouldBe "Change uk phone number"

        val continue = doc.select("a.govuk-button")
        continue.text()       shouldBe "Continue"
        continue.attr("href") shouldBe addedLiaisonOfficerEndpoint
        doc.text()              should include("Is this page not working properly?")
      }
    }

    "redirect when the liaison officer cannot be found" in {
      val application =
        applicationBuilder(effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq.empty)))).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

    "redirect when the signatory details are incomplete" in {
      val incomplete  = testLiaisonOfficer.copy(communication = Set.empty)
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(incomplete))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, url)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
      }
    }

  }
}
