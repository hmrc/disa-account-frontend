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
import org.mockito.Mockito.when
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import utils.BaseUnitSpec

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class ManageIsasControllerSpec extends BaseUnitSpec {

  private val fixedClock: Clock =
    Clock.fixed(Instant.parse("2026-07-08T09:00:00Z"), ZoneOffset.UTC)

  private val monthlyReportSubmissionUrl: String =
    "http://localhost:1205/obligations/returns/isa/monthly-report-submission"

  private def application(windowOpen: Boolean, registrationDetails: Option[RegistrationDetails]) = {
    when(mockRegistrationConnector.getRegistrationDetails(any())(any()))
      .thenReturn(Future.successful(registrationDetails))
    when(mockReportingWindowConnector.isReportingWindowOpen(any())(any())).thenReturn(Future.successful(windowOpen))

    applicationBuilder()
      .overrides(bind[Clock].toInstance(fixedClock))
      .build()
  }

  "ManageIsasController.onPageLoad" should {

    "show the closed reporting window content, without a notification banner" in {
      val app = application(windowOpen = false, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                   shouldBe OK
        doc.select("h1").text()                          shouldBe s"Manage $testCompanyName ISAs"
        doc.text()                                         should include("The window for submitting your monthly report is now closed.")
        doc.text()                                         should include("Reporting period for July is closed.")
        doc.select(".govuk-notification-banner").isEmpty shouldBe true
      }
    }

    "show the open reporting window content, with a notification banner" in {
      val app = application(windowOpen = true, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                   shouldBe OK
        doc.select("h1").text()                          shouldBe s"Manage $testCompanyName ISAs"
        doc.text()                                         should include(
          "The monthly reporting period for July is now open. You can upload your June report. It closes at 11:59pm on 19 July."
        )
        doc.text()                                         should include("You have 11 days left to submit your monthly report.")
        doc.text()                                         should include(
          "The window for submitting your monthly report is now open. You can either download the Excel template file or continue to upload your monthly report."
        )
        doc.text()                                         should include("Reporting period for July is open. Upload your June report.")
        doc.select(".govuk-notification-banner").isEmpty shouldBe false
      }
    }

    "show 'Submit report' as a plain heading, not a link" in {
      val app = application(windowOpen = true, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        val heading = doc.select("h2:contains(Submit report)")
        heading.text()              shouldBe "Submit report"
        heading.select("a").isEmpty shouldBe true
      }
    }

    "link 'Upload your <month> report' to the disa-returns-frontend monthly report submission page when the window is open" in {
      val app = application(windowOpen = true, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        val submitReportLink = doc.select("a:contains(Upload your June report)")
        submitReportLink.attr("href") shouldBe monthlyReportSubmissionUrl
      }
    }

    "not show an upload report link when the window is closed" in {
      val app = application(windowOpen = false, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select(s"""a[href="$monthlyReportSubmissionUrl"]""").isEmpty shouldBe true
      }
    }

    "show the change-under-review warning when an ISA products change is under review" in {
      val underReview = testRegistrationDetails.copy(isaProductsChangeUnderReview = true)
      val app         = application(windowOpen = false, registrationDetails = Some(underReview))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.text()                                                                     should include("Change of circumstances")
        doc.text()                                                                     should include(
          "You changed the ISA products your organisation offers. This is being checked by HMRC."
        )
        doc.text()                                                                     should include(
          "You will not be able to make any further changes until HMRC have agreed to the current changes."
        )
        doc.text()                                                                     should include("You will receive a letter from HMRC once the changes have been approved.")
        doc.select("a:contains(Update information about your organisation)").isEmpty shouldBe true
      }
    }

    "show the change of circumstances link when no ISA products change is under review" in {
      val app = application(windowOpen = false, registrationDetails = Some(testRegistrationDetails))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.text()        should include("Change of circumstances")
        doc.text()        should include(
          "Only administrators can carry out any change of circumstances that relate to the organisation."
        )
        doc
          .select("a:contains(Update information about your organisation)")
          .attr("href") shouldBe changeInformationEndpoint
      }
    }

    "return an internal server error page when no registration is found" in {
      val app = application(windowOpen = false, registrationDetails = None)

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "return an internal server error page when the registration has no company name" in {
      val noCompanyName = testRegistrationDetails.copy(businessVerification = None)
      val app           = application(windowOpen = false, registrationDetails = Some(noCompanyName))

      running(app) {
        val result = route(app, FakeRequest(GET, manageIsasEndpoint)).value

        status(result) shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
