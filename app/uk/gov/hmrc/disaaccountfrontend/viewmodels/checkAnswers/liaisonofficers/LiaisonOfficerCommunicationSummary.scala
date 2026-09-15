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

package uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.liaisonofficers

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.controllers.liaisonofficers.routes.LiaisonOfficerCommunicationController
import uk.gov.hmrc.disaaccountfrontend.models.CheckMode
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication}
import uk.gov.hmrc.govukfrontend.views.Aliases.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

object LiaisonOfficerCommunicationSummary {

  def row(officer: LiaisonOfficer)(implicit messages: Messages): Option[SummaryListRow] =
    Some(
      SummaryListRow(
        key = Key(Text(messages("liaisonOfficerCommunication.checkYourAnswersLabel"))),
        value = Value(Text(stringifiedPref(officer.communication))),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = LiaisonOfficerCommunicationController.onPageLoad(officer.id, CheckMode).url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("liaisonOfficerCommunication.change.hidden"))
              )
            )
          )
        )
      )
    )

  private def stringifiedPref(preferences: Set[LiaisonOfficerCommunication]): String = {

    val str: Seq[String] = preferences.toSeq.map(_.toString.toLowerCase.replaceAll("by", "by ").trim)

    val capitalised = str match {
      case head +: tail => head.capitalize +: tail
    }

    capitalised.mkString(", ")
  }
}
