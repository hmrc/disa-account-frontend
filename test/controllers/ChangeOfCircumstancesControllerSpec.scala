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
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{AuthorisedUsers, OrganisationInformation, ViewAllInformation}
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles.FcaArticle14
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, CashJuniorIsas, InnovativeFinanceIsas, StocksAndSharesIsas}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, ChangeInformationSelection, CorrespondenceAddress, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class ChangeOfCircumstancesControllerSpec extends BaseUnitSpec {

  private val signatoryEmail = "jane@example.com"

  private val fullAnswers = Answers(
    tradingName = Some("ABC Bank"),
    correspondenceAddress = Some(
      CorrespondenceAddress(Some("123 Number Road"), Some("Fake Town"), None, Some("AB1 BA1"))
    ),
    organisationTelephoneNumber = Some("123456789"),
    organisationEmailAddress = Some("abc@xyz.co"),
    isaProducts = Some(Seq(StocksAndSharesIsas)),
    fcaArticles = Some(Seq(FcaArticle14)),
    liaisonOfficers = Some(
      LiaisonOfficers(
        Seq(
          LiaisonOfficer(
            "lo-1",
            Some("John Smith"),
            Some("0123"),
            Set(LiaisonOfficerCommunication.values.head),
            Some("john@example.com")
          )
        )
      )
    ),
    signatories = Some(
      Signatories(
        Seq(
          Signatory("s-1", Some("Jane Doe"), Some("Director"), Some(signatoryEmail)),
          Signatory("s-2", Some("Joe Blogs"), Some("Director"))
        )
      )
    )
  )

  private def sessionWith(selections: ChangeInformationSelection*): Option[UserAnswers] =
    Some(
      UserAnswers(
        testSessionId,
        SessionUpdates(changeInformationSelections = Assign(selections))
      )
    )

  "ChangeOfCircumstancesController.onPageLoad" should {

    "display the current information under each section with change links" in {
      val application = applicationBuilder(effectiveAnswers = fullAnswers, email = Some(signatoryEmail)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                   shouldBe OK
        doc.select("h1").text()                          shouldBe "Manage organisation information"
        doc.select("main h2").eachText()                      shouldBe java.util.List.of(
          "Organisation details",
          "Product information",
          "Authorised users"
        )
        doc.select(".govuk-summary-list__key").eachText() shouldBe java.util.List.of(
          "Trading name",
          "Added correspondence address",
          "Organisation telephone number",
          "Organisation email",
          "Products",
          "Articles",
          "Liaison officer",
          "Signatory"
        )
        doc.select(".govuk-summary-list__value").eachText() should contain("ABC Bank")
        doc.select(".govuk-summary-list__value").eachText() should contain("123 Number Road Fake Town AB1 BA1")
        doc.select(".govuk-summary-list__value").eachText() should contain("Jane Doe Joe Blogs")
        doc.select(".govuk-summary-list__actions a").size() shouldBe 8

        doc.select(".govuk-summary-list__actions a").eachAttr("href") shouldBe java.util.List.of(
          "/obligations/account/isa/trading-name",
          "/obligations/account/isa/enter-your-organisation-address",
          "/obligations/account/isa/organisation-telephone-number",
          "/obligations/account/isa/organisation-email-address",
          "/obligations/account/isa/change-products",
          "/obligations/account/isa/fca-articles",
          "/obligations/account/isa/added-liaison-officers",
          "/obligations/account/isa/added-signatories"
        )
      }
    }

    "link to select a different option and back to the Manage ISAs homepage" in {
      val application = applicationBuilder(effectiveAnswers = fullAnswers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("a:contains(select a different option)").attr("href") shouldBe changeInformationEndpoint
        doc.select("a:contains(return to the Manage ISAs homepage)").attr("href") shouldBe "/"
      }
    }

    "only show the sections the user selected" in {
      val application = applicationBuilder(
        effectiveAnswers = fullAnswers,
        sessionAnswers = sessionWith(OrganisationInformation)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("main h2").eachText() shouldBe java.util.List.of("Organisation details")
      }
    }

    "show every section for a signatory who selected view all information" in {
      val application = applicationBuilder(
        effectiveAnswers = fullAnswers,
        sessionAnswers = sessionWith(ViewAllInformation),
        email = Some(signatoryEmail)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("main h2").size() shouldBe 3
      }
    }

    "not show product information to a user who is not a signatory" in {
      val application = applicationBuilder(
        effectiveAnswers = fullAnswers,
        sessionAnswers = sessionWith(ViewAllInformation, AuthorisedUsers)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("main h2").eachText() shouldBe java.util.List.of("Organisation details", "Authorised users")
      }
    }

    "not show the check your changes section when nothing changed" in {
      val application = applicationBuilder(effectiveAnswers = fullAnswers, email = Some(signatoryEmail)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("main").text() should not include "Check your changes"
        doc.select("main").text() should not include "ISA product changes information"
        doc.select("main .govuk-inset-text").size() shouldBe 0
      }
    }

    "show changed organisation fields, and none of them when they haven't changed" in {
      val original = Answers(
        tradingName = Some("ABC Bank"),
        organisationTelephoneNumber = Some("111"),
        organisationEmailAddress = Some("old@example.com")
      )
      val effective = Answers(
        tradingName = Some("XYZ Bank"),
        organisationTelephoneNumber = Some("111"),
        organisationEmailAddress = Some("new@example.com")
      )
      val application = applicationBuilder(effectiveAnswers = effective, originalAnswers = Some(original)).build()

      running(application) {
        val result           = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc              = Jsoup.parse(contentAsString(result))
        val checkYourChanges = doc.select(".govuk-summary-list").last()

        doc.select("main h2").eachText()                                shouldBe
          java.util.List.of("Organisation details", "Check your changes")
        checkYourChanges.select(".govuk-summary-list__key").eachText()   shouldBe
          java.util.List.of("Changed trading name", "Changed organisation email")
        checkYourChanges.select(".govuk-summary-list__value").eachText() shouldBe
          java.util.List.of("ABC Bank to XYZ Bank", "old@example.com to new@example.com")
      }
    }

    "show signatories added and removed, without the ISA product changes information" in {
      val original = Answers(
        signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director")))))
      )
      val effective = Answers(
        signatories = Some(Signatories(Seq(Signatory("s-2", Some("Joe Blogs"), Some("Director")))))
      )
      val application = applicationBuilder(effectiveAnswers = effective, originalAnswers = Some(original)).build()

      running(application) {
        val result           = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc              = Jsoup.parse(contentAsString(result))
        val checkYourChanges = doc.select(".govuk-summary-list").last()

        doc.select("main h2").eachText()                            shouldBe
          java.util.List.of("Authorised users", "Check your changes")
        checkYourChanges.select(".govuk-summary-list__key").eachText()   shouldBe
          java.util.List.of("Signatories added", "Signatories removed")
        checkYourChanges.select(".govuk-summary-list__value").eachText() shouldBe
          java.util.List.of("Joe Blogs", "Jane Doe")
      }
    }

    "show liaison officers added and removed" in {
      val original = Answers(
        liaisonOfficers = Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                "lo-1",
                Some("John Smith"),
                Some("0123"),
                Set(LiaisonOfficerCommunication.values.head),
                Some("john@example.com")
              )
            )
          )
        )
      )
      val effective = Answers(
        liaisonOfficers = Some(
          LiaisonOfficers(
            Seq(
              LiaisonOfficer(
                "lo-2",
                Some("Amanda Jones"),
                Some("0456"),
                Set(LiaisonOfficerCommunication.values.head),
                Some("amanda@example.com")
              )
            )
          )
        )
      )
      val application = applicationBuilder(effectiveAnswers = effective, originalAnswers = Some(original)).build()

      running(application) {
        val result           = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc              = Jsoup.parse(contentAsString(result))
        val checkYourChanges = doc.select(".govuk-summary-list").last()

        doc.select("main h2").eachText()                            shouldBe
          java.util.List.of("Authorised users", "Check your changes")
        checkYourChanges.select(".govuk-summary-list__key").eachText()   shouldBe
          java.util.List.of("Liaison officers added", "Liaison officers removed")
        checkYourChanges.select(".govuk-summary-list__value").eachText() shouldBe
          java.util.List.of("Amanda Jones", "John Smith")
      }
    }

    "show products added and removed, and the manual processing information" in {
      val original  = Answers(isaProducts = Some(Seq(StocksAndSharesIsas, InnovativeFinanceIsas)))
      val effective = Answers(isaProducts = Some(Seq(CashIsas, CashJuniorIsas)))
      val application = applicationBuilder(effectiveAnswers = effective, originalAnswers = Some(original)).build()

      running(application) {
        val result           = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc              = Jsoup.parse(contentAsString(result))
        val checkYourChanges = doc.select(".govuk-summary-list").last()

        doc.select("main h2").eachText() shouldBe java.util.List.of(
          "Check your changes",
          "ISA product changes information"
        )
        checkYourChanges.select(".govuk-summary-list__key").eachText()   shouldBe
          java.util.List.of("Products added", "Products removed")
        checkYourChanges.select(".govuk-summary-list__value").eachText() shouldBe
          java.util.List.of("Cash ISAs Cash Junior ISAs", "Stocks and Shares ISAs Innovative Finance ISAs")
        doc.select("main").text() should include(
          "Any changes to ISA products will require manual processing once submitted and you will be unable to make any additional changes until you hear from HMRC. We aim to confirm the changes by email, within 2 weeks."
        )
        doc.select("main").text() should include(
          "If you have other changes to make, for example, to organisation details or authorised users - you can do these before you make ISA product changes and submit them."
        )
        doc.select("main .govuk-inset-text").size() shouldBe 1
      }
    }

    "only show products removed when products were only removed" in {
      val original    = Answers(isaProducts = Some(Seq(CashIsas, StocksAndSharesIsas)))
      val effective   = Answers(isaProducts = Some(Seq(CashIsas)))
      val application = applicationBuilder(effectiveAnswers = effective, originalAnswers = Some(original)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select(".govuk-summary-list").last().select(".govuk-summary-list__key").eachText() shouldBe
          java.util.List.of("Products removed")
      }
    }

    "link the continue button to the declaration" in {
      val application = applicationBuilder(effectiveAnswers = fullAnswers).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeOfCircumstancesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        doc.select("a.govuk-button").attr("href") shouldBe declarationForChangesEndpoint
      }
    }
  }
}
