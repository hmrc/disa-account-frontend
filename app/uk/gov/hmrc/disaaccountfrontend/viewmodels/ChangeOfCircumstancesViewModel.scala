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

package uk.gov.hmrc.disaaccountfrontend.viewmodels

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.{AuthorisedUsers, IsaProductInformation, OrganisationInformation}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, ChangeInformationSelection}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances.*
import uk.gov.hmrc.govukfrontend.views.Aliases.{SummaryList, SummaryListRow}

final case class ChangeOfCircumstancesViewModel(
  organisation: Option[SummaryList],
  products: Option[SummaryList],
  authorisedUsers: Option[SummaryList],
  checkYourChanges: Option[SummaryList],
  isaProductsChanged: Boolean
)

object ChangeOfCircumstancesViewModel {

  def apply(
    originalAnswers: Answers,
    effectiveAnswers: Answers,
    selections: Seq[ChangeInformationSelection],
    isSignatory: Boolean
  )(implicit messages: Messages): ChangeOfCircumstancesViewModel = {

    def shown(section: ChangeInformationSelection): Boolean =
      ChangeInformationSelection.isShown(selections, section)

    val organisation = Option
      .when(shown(OrganisationInformation))(
        summaryList(
          TradingNameSummary.row(effectiveAnswers),
          CorrespondenceAddressSummary.row(effectiveAnswers),
          OrganisationTelephoneNumberSummary.row(effectiveAnswers),
          OrganisationEmailSummary.row(effectiveAnswers)
        )
      )
      .filter(_.rows.nonEmpty)

    val products = Option
      .when(shown(IsaProductInformation) && isSignatory)(
        summaryList(
          IsaProductsSummary.row(effectiveAnswers),
          InnovativeFinancialProductsSummary.row(effectiveAnswers),
          PeerToPeerPlatformSummary.row(effectiveAnswers),
          PeerToPeerPlatformNumberSummary.row(effectiveAnswers),
          FcaArticlesSummary.row(effectiveAnswers).orElse(FinancialOrganisationSummary.row(effectiveAnswers))
        )
      )
      .filter(_.rows.nonEmpty)

    val authorisedUsers = Option
      .when(shown(AuthorisedUsers))(
        summaryList(
          LiaisonOfficersSummary.row(effectiveAnswers),
          if (isSignatory) SignatoriesSummary.row(effectiveAnswers) else None
        )
      )
      .filter(_.rows.nonEmpty)

    val productChanges = ProductChanges(originalAnswers, effectiveAnswers)

    val isaProductChangeRows = if (isSignatory) {
      productChanges.rows ++
        InnovativeFinancialProductsChanges(originalAnswers, effectiveAnswers).rows ++
        FieldChanges.isaProductFieldRows(originalAnswers, effectiveAnswers) ++
        FcaArticlesChanges(originalAnswers, effectiveAnswers).rows ++
        FinancialOrganisationChanges(originalAnswers, effectiveAnswers).rows
    } else {
      Seq.empty
    }

    val signatoryChangeRows   = if (isSignatory) SignatoryChanges(originalAnswers, effectiveAnswers).rows else Seq.empty
    val liaisonOfficerChanges = LiaisonOfficerChanges(originalAnswers, effectiveAnswers)
    val checkYourChangesRows  =
      FieldChanges.rows(originalAnswers, effectiveAnswers) ++
        isaProductChangeRows ++ signatoryChangeRows ++ liaisonOfficerChanges.rows

    ChangeOfCircumstancesViewModel(
      organisation = organisation,
      products = products,
      authorisedUsers = authorisedUsers,
      checkYourChanges = Option.when(checkYourChangesRows.nonEmpty)(SummaryList(rows = checkYourChangesRows)),
      isaProductsChanged = productChanges.hasChanges
    )
  }

  private def summaryList(rows: Option[SummaryListRow]*): SummaryList =
    SummaryList(rows = rows.flatten)
}
