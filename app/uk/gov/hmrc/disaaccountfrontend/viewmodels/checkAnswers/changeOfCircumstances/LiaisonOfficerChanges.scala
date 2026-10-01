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
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication}
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

final case class LiaisonOfficerUpdate(original: LiaisonOfficer, effective: LiaisonOfficer) {

  def rows(implicit messages: Messages): Seq[SummaryListRow] = {
    def row(headingKey: String, oldValue: String, newValue: String) =
      Option.when(oldValue != newValue)(ChangesSummaryRow.valueChange(headingKey, oldValue, newValue))

    Seq(
      row(
        "changeOfCircumstances.checkYourChanges.changedLiaisonOfficerName",
        original.fullName.getOrElse(""),
        effective.fullName.getOrElse("")
      ),
      row(
        "changeOfCircumstances.checkYourChanges.changedLiaisonOfficerEmail",
        original.email.getOrElse(""),
        effective.email.getOrElse("")
      ),
      row(
        "changeOfCircumstances.checkYourChanges.changedLiaisonOfficerPhoneNumber",
        original.phoneNumber.getOrElse(""),
        effective.phoneNumber.getOrElse("")
      ),
      row(
        "changeOfCircumstances.checkYourChanges.changedLiaisonOfficerCommunication",
        LiaisonOfficerUpdate.communication(original.communication),
        LiaisonOfficerUpdate.communication(effective.communication)
      )
    ).flatten
  }
}

object LiaisonOfficerUpdate {

  private def communication(selected: Set[LiaisonOfficerCommunication])(implicit messages: Messages): String =
    LiaisonOfficerCommunication.values
      .filter(selected)
      .map(value => messages(s"liaisonOfficerCommunication.$value"))
      .mkString(", ")
}

final case class LiaisonOfficerChanges(
  added: Seq[String],
  removed: Seq[String],
  updated: Seq[LiaisonOfficerUpdate] = Seq.empty
) {

  val hasChanges: Boolean = added.nonEmpty || removed.nonEmpty || updated.nonEmpty

  def rows(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      Option.when(added.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.liaisonOfficersAdded", added)
      ),
      Option.when(removed.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.liaisonOfficersRemoved", removed)
      )
    ).flatten ++ updated.flatMap(_.rows)
}

object LiaisonOfficerChanges {

  def apply(original: Answers, effective: Answers): LiaisonOfficerChanges = {
    val originalOfficers: Seq[LiaisonOfficer]  =
      original.liaisonOfficers.toSeq.flatMap(_.liaisonOfficers).filter(_.isComplete)
    val effectiveOfficers: Seq[LiaisonOfficer] =
      effective.liaisonOfficers.toSeq.flatMap(_.liaisonOfficers).filter(_.isComplete)

    val originalIds    = originalOfficers.map(_.id).toSet
    val effectiveIds   = effectiveOfficers.map(_.id).toSet
    val originalNames  = originalOfficers.flatMap(_.fullName).toSet
    val effectiveNames = effectiveOfficers.flatMap(_.fullName).toSet

    def isUnchanged(officer: LiaisonOfficer, matchingIds: Set[String], matchingNames: Set[String]): Boolean =
      matchingIds(officer.id) || officer.fullName.exists(matchingNames)

    def originalLo(officer: LiaisonOfficer): Option[LiaisonOfficer] =
      originalOfficers
        .find(_.id == officer.id)
        .orElse(originalOfficers.find(o => o.fullName.isDefined && o.fullName == officer.fullName))

    LiaisonOfficerChanges(
      added = effectiveOfficers.filterNot(isUnchanged(_, originalIds, originalNames)).flatMap(_.fullName),
      removed = originalOfficers.filterNot(isUnchanged(_, effectiveIds, effectiveNames)).flatMap(_.fullName),
      updated = effectiveOfficers.flatMap { officer =>
        originalLo(officer)
          .filter(_.copy(id = officer.id) != officer)
          .map(LiaisonOfficerUpdate(_, officer))
      }
    )
  }
}
