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

package uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

final case class FinancialOrganisationChanges(added: Seq[FinancialOrganisation], removed: Seq[FinancialOrganisation]) {

  val hasChanges: Boolean = added.nonEmpty || removed.nonEmpty

  def addedNames(implicit messages: Messages): Seq[String]   = added.map(FinancialOrganisationSummary.organisationName)
  def removedNames(implicit messages: Messages): Seq[String] =
    removed.map(FinancialOrganisationSummary.organisationName)

  def rows(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      Option.when(added.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.organisationDescriptionAdded", addedNames)
      ),
      Option.when(removed.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.organisationDescriptionRemoved", removedNames)
      )
    ).flatten
}

object FinancialOrganisationChanges {

  def apply(original: Answers, effective: Answers): FinancialOrganisationChanges = {
    val originalOrganisations  = original.financialOrganisation.getOrElse(Seq.empty).toSet
    val effectiveOrganisations = effective.financialOrganisation.getOrElse(Seq.empty).toSet

    FinancialOrganisationChanges(
      added = FinancialOrganisation.values.filter(organisation =>
        effectiveOrganisations(organisation) && !originalOrganisations(organisation)
      ),
      removed = FinancialOrganisation.values.filter(organisation =>
        originalOrganisations(organisation) && !effectiveOrganisations(organisation)
      )
    )
  }
}
