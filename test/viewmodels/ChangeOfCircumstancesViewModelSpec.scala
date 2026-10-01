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

package viewmodels

import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{AuthorisedUsers, IsaProductInformation, OrganisationInformation, ViewAllInformation}
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles.{FcaArticle14, FcaArticle21}
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation.{Bank, BuildingSociety}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas, StocksAndSharesIsas}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.viewmodels.ChangeOfCircumstancesViewModel
import utils.BaseUnitSpec

class ChangeOfCircumstancesViewModelSpec extends BaseUnitSpec {

  private val organisationAnswers =
    Answers(tradingName = Some("ABC Bank"), organisationTelephoneNumber = Some("123456789"))

  "ChangeOfCircumstancesViewModel" should {

    "show every section when no selections have been made yet" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = organisationAnswers,
        effectiveAnswers = organisationAnswers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.organisation  shouldBe defined
      viewModel.authorisedUsers should not be defined
    }

    "only show the selected sections" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = organisationAnswers,
        effectiveAnswers = organisationAnswers,
        selections = Seq(OrganisationInformation),
        isSignatory = true
      )(messages(app))

      viewModel.organisation  shouldBe defined
      viewModel.products        should not be defined
      viewModel.authorisedUsers should not be defined
    }

    "show every section when view all information is selected" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = organisationAnswers,
        effectiveAnswers = organisationAnswers.copy(isaProducts = Some(Seq(CashIsas))),
        selections = Seq(ViewAllInformation),
        isSignatory = true
      )(messages(app))

      viewModel.organisation shouldBe defined
      viewModel.products     shouldBe defined
    }

    "not show product information to a user who is not a signatory, even when selected" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = Answers(),
        effectiveAnswers = Answers(isaProducts = Some(Seq(CashIsas))),
        selections = Seq(IsaProductInformation),
        isSignatory = false
      )(messages(app))

      viewModel.products should not be defined
    }

    "show FCA articles when present, and no organisation description row alongside it" in {
      val answers = Answers(
        isaProducts = Some(Seq(CashIsas)),
        fcaArticles = Some(Seq(FcaArticle14)),
        financialOrganisation = Some(Seq(Bank))
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.products.value.rows.map(_.key.content.asHtml.body) shouldBe Seq("Products", "Articles")
    }

    "show the organisation description when there are no FCA articles" in {
      val answers = Answers(
        isaProducts = Some(Seq(CashIsas)),
        financialOrganisation = Some(Seq(Bank))
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.products.value.rows.map(_.key.content.asHtml.body) shouldBe
        Seq("Products", "Organisation description")
    }

    "show innovative finance products, platform name and platform FCA FRN when present" in {
      val answers = Answers(
        isaProducts = Some(Seq(IsaProduct.CashIsas, IsaProduct.StocksAndSharesIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.CrowdFundedDebentures)),
        p2pPlatform = Some("Test platform name"),
        p2pPlatformNumber = Some("1234567")
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      val rows = viewModel.products.value.rows
      rows.map(_.key.content.asHtml.body) shouldBe
        Seq(
          "Products",
          "Innovative Finance ISAs type",
          "Innovative Finance ISAs platform",
          "Innovative finance ISAs platform FCA, FRN"
        )
      rows.map(_.value.content.asHtml.body) should contain allOf (
        "Crowdfunded debentures",
        "Test platform name",
        "1234567"
      )
    }

    "not show innovative finance products, platform name or platform FCA FRN when absent" in {
      val answers = Answers(isaProducts = Some(Seq(CashIsas)))

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.products.value.rows.map(_.key.content.asHtml.body) shouldBe Seq("Products")
    }

    "not show organisation description or FCA articles to a non-signatory" in {
      val answers = Answers(financialOrganisation = Some(Seq(Bank)), fcaArticles = Some(Seq(FcaArticle14)))

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = false
      )(messages(app))

      viewModel.products should not be defined
    }

    "not show a section with nothing to display, even when selected" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = Answers(),
        effectiveAnswers = Answers(),
        selections = Seq(AuthorisedUsers),
        isSignatory = true
      )(messages(app))

      viewModel.authorisedUsers should not be defined
    }

    "have no check-your-changes summary or ISA product change flag when nothing changed" in {
      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = organisationAnswers,
        effectiveAnswers = organisationAnswers,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.checkYourChanges     should not be defined
      viewModel.isaProductsChanged shouldBe false
    }

    "only show liaison officers, not signatories, within authorised users to a non-signatory" in {
      val answers = Answers(
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
        signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director")))))
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = answers,
        effectiveAnswers = answers,
        selections = Seq.empty,
        isSignatory = false
      )(messages(app))

      viewModel.authorisedUsers.value.rows.map(_.key.content.asHtml.body) shouldBe Seq("Liaison officer")
    }

    "not include signatories added or removed in check-your-changes for a non-signatory" in {
      val original  = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))
      val effective =
        Answers(signatories = Some(Signatories(Seq(Signatory("s-2", Some("Joe Blogs"), Some("Director"))))))

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = original,
        effectiveAnswers = effective,
        selections = Seq.empty,
        isSignatory = false
      )(messages(app))

      viewModel.checkYourChanges should not be defined
    }

    "include innovative finance products, platform, articles and organisation description changes in check-your-changes" in {
      val original  = Answers(
        isaProducts = Some(Seq(StocksAndSharesIsas)),
        innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.PeertopeerLoansAndHave36hPermissions)),
        fcaArticles = Some(Seq(FcaArticle14)),
        financialOrganisation = Some(Seq(Bank)),
        p2pPlatform = Some("Old platform"),
        p2pPlatformNumber = Some("1111111")
      )
      val effective = Answers(
        isaProducts = Some(Seq(CashIsas)),
        innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.CrowdFundedDebentures)),
        fcaArticles = Some(Seq(FcaArticle21)),
        financialOrganisation = Some(Seq(BuildingSociety)),
        p2pPlatform = Some("New platform"),
        p2pPlatformNumber = Some("2222222")
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = original,
        effectiveAnswers = effective,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.checkYourChanges.value.rows.map(_.key.content.asHtml.body) shouldBe Seq(
        "Products added",
        "Products removed",
        "Innovative finance products added",
        "Innovative finance products removed",
        "Changed innovative finance ISAs platform",
        "Changed innovative finance ISAs platform FCA, FRN",
        "Articles added",
        "Articles removed",
        "Organisation description added",
        "Organisation description removed"
      )
    }

    "not include innovative finance products, platform, articles or organisation description changes for a non-signatory" in {
      val original  = Answers(
        innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.PeertopeerLoansAndHave36hPermissions)),
        fcaArticles = Some(Seq(FcaArticle14)),
        financialOrganisation = Some(Seq(Bank)),
        p2pPlatform = Some("Old platform"),
        p2pPlatformNumber = Some("1111111")
      )
      val effective = Answers(
        innovativeFinancialProducts = Some(Seq(InnovativeFinancialProduct.CrowdFundedDebentures)),
        fcaArticles = Some(Seq(FcaArticle21)),
        financialOrganisation = Some(Seq(BuildingSociety)),
        p2pPlatform = Some("New platform"),
        p2pPlatformNumber = Some("2222222")
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = original,
        effectiveAnswers = effective,
        selections = Seq.empty,
        isSignatory = false
      )(messages(app))

      viewModel.checkYourChanges should not be defined
    }

    "combine field, product and signatory changes into a single check-your-changes summary" in {
      val original  = organisationAnswers.copy(isaProducts = Some(Seq(StocksAndSharesIsas)))
      val effective = organisationAnswers.copy(
        tradingName = Some("XYZ Bank"),
        isaProducts = Some(Seq(CashIsas)),
        signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director")))))
      )

      val viewModel = ChangeOfCircumstancesViewModel(
        originalAnswers = original,
        effectiveAnswers = effective,
        selections = Seq.empty,
        isSignatory = true
      )(messages(app))

      viewModel.checkYourChanges.value.rows should have size 4
      viewModel.isaProductsChanged        shouldBe true
    }
  }
}
