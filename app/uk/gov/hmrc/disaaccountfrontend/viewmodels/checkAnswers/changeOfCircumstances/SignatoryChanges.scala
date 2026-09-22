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

final case class SignatoryChanges(added: Seq[String], removed: Seq[String]) {

  val hasChanges: Boolean = added.nonEmpty || removed.nonEmpty

  def rows(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      Option.when(added.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.signatoriesAdded", added)
      ),
      Option.when(removed.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.signatoriesRemoved", removed)
      )
    ).flatten
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

    // A signatory counts as unchanged if it matches by id, or - since the id we're given for an
    // untouched signatory isn't guaranteed to stay the same between the original and effective
    // answers - by full name, so the same person is never reported as both added and removed.
    def isUnchanged(signatory: Signatory, matchingIds: Set[String], matchingNames: Set[String]): Boolean =
      matchingIds(signatory.id) || signatory.fullName.exists(matchingNames)

    SignatoryChanges(
      added = effectiveSignatories.filterNot(isUnchanged(_, originalIds, originalNames)).flatMap(_.fullName),
      removed = originalSignatories.filterNot(isUnchanged(_, effectiveIds, effectiveNames)).flatMap(_.fullName)
    )
  }
}
