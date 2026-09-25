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
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CorrespondenceAddress}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

// Rows for the simple, single-value organisation fields: "Changed <field>" / "<old> to <new>".
// Fields with their own added/removed semantics (ISA products, signatories, liaison officers)
// are handled separately by ProductChanges, SignatoryChanges and LiaisonOfficerChanges.
object FieldChanges {

  def rows(original: Answers, effective: Answers)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      fieldRow(
        "changeOfCircumstances.checkYourChanges.changedTradingName",
        original.tradingName,
        effective.tradingName
      ),
      addressRow(original.correspondenceAddress, effective.correspondenceAddress),
      fieldRow(
        "changeOfCircumstances.checkYourChanges.changedOrganisationTelephoneNumber",
        original.organisationTelephoneNumber,
        effective.organisationTelephoneNumber
      ),
      fieldRow(
        "changeOfCircumstances.checkYourChanges.changedOrganisationEmailAddress",
        original.organisationEmailAddress,
        effective.organisationEmailAddress
      )
    ).flatten

  // Simple, single-value ISA product fields - kept separate from `rows` above since these are only
  // ever shown to signatories, unlike the organisation fields.
  def isaProductFieldRows(original: Answers, effective: Answers)(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      fieldRow(
        "changeOfCircumstances.checkYourChanges.changedPlatformName",
        original.p2pPlatform,
        effective.p2pPlatform
      ),
      fieldRow(
        "changeOfCircumstances.checkYourChanges.changedPlatformFcaFrn",
        original.p2pPlatformNumber,
        effective.p2pPlatformNumber
      )
    ).flatten

  private def fieldRow(headingKey: String, oldValue: Option[String], newValue: Option[String])(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    Option.when(newValue.isDefined && oldValue != newValue)(
      ChangesSummaryRow(
        headingKey,
        Seq(
          messages(
            "changeOfCircumstances.checkYourChanges.valueChange",
            oldValue.getOrElse(""),
            newValue.getOrElse("")
          )
        )
      )
    )

  // Shown the same way as an added address (each line of its own), but with the old address's lines,
  // a "to" line, then the new address's lines, rather than a single "<old> to <new>" line - an address
  // read better broken across lines than run together.
  private def addressRow(
    oldAddress: Option[CorrespondenceAddress],
    newAddress: Option[CorrespondenceAddress]
  )(implicit messages: Messages): Option[SummaryListRow] =
    Option.when(newAddress.isDefined && oldAddress != newAddress) {
      val oldLines = oldAddress.toSeq.flatMap(CorrespondenceAddressSummary.lines)
      val newLines = newAddress.toSeq.flatMap(CorrespondenceAddressSummary.lines)

      ChangesSummaryRow(
        "changeOfCircumstances.checkYourChanges.changedCorrespondenceAddress",
        oldLines ++ Seq(messages("changeOfCircumstances.checkYourChanges.to")) ++ newLines
      )
    }
}
