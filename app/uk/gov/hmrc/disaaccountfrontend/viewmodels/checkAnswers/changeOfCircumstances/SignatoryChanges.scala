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
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

final case class SignatoryUpdate(original: Signatory, effective: Signatory) {

  def rows(implicit messages: Messages): Seq[SummaryListRow] = {
    def row(headingKey: String, oldValue: Option[String], newValue: Option[String]) =
      Option.when(oldValue != newValue)(
        ChangesSummaryRow.valueChange(headingKey, oldValue.getOrElse(""), newValue.getOrElse(""))
      )

    Seq(
      row("changeOfCircumstances.checkYourChanges.changedSignatoryName", original.fullName, effective.fullName),
      row("changeOfCircumstances.checkYourChanges.changedSignatoryJobTitle", original.jobTitle, effective.jobTitle)
    ).flatten
  }
}

final case class SignatoryChanges(added: Seq[String], removed: Seq[String], updated: Seq[SignatoryUpdate] = Seq.empty) {

  val hasChanges: Boolean = added.nonEmpty || removed.nonEmpty || updated.nonEmpty

  def rows(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      Option.when(added.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.signatoriesAdded", added)
      ),
      Option.when(removed.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.signatoriesRemoved", removed)
      )
    ).flatten ++ updated.flatMap(_.rows)
}

object SignatoryChanges {

  def apply(original: Answers, effective: Answers): SignatoryChanges = {
    val originalSignatories: Seq[Signatory]  =
      original.signatories.toSeq.flatMap(_.signatories).filter(_.isComplete)
    val effectiveSignatories: Seq[Signatory] =
      effective.signatories.toSeq.flatMap(_.signatories).filter(_.isComplete)

    val originalIds    = originalSignatories.map(_.id).toSet
    val effectiveIds   = effectiveSignatories.map(_.id).toSet
    val originalNames  = originalSignatories.flatMap(_.fullName).toSet
    val effectiveNames = effectiveSignatories.flatMap(_.fullName).toSet

    def isUnchanged(signatory: Signatory, matchingIds: Set[String], matchingNames: Set[String]): Boolean =
      matchingIds(signatory.id) || signatory.fullName.exists(matchingNames)

    def originalSignatory(signatory: Signatory): Option[Signatory] =
      originalSignatories
        .find(_.id == signatory.id)
        .orElse(originalSignatories.find(s => s.fullName.isDefined && s.fullName == signatory.fullName))

    SignatoryChanges(
      added = effectiveSignatories.filterNot(isUnchanged(_, originalIds, originalNames)).flatMap(_.fullName),
      removed = originalSignatories.filterNot(isUnchanged(_, effectiveIds, effectiveNames)).flatMap(_.fullName),
      updated = effectiveSignatories.flatMap { signatory =>
        originalSignatory(signatory)
          .filter(o => o.fullName != signatory.fullName || o.jobTitle != signatory.jobTitle)
          .map(SignatoryUpdate(_, signatory))
      }
    )
  }
}
