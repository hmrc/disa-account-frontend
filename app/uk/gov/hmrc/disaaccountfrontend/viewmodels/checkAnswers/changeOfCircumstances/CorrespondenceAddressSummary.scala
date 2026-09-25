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
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.EnterYourOrganisationAddressController
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CorrespondenceAddress}
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

object CorrespondenceAddressSummary {

  def row(answers: Answers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.correspondenceAddress.filter(hasAnyLine).map { address =>
      SummaryListRow(
        key = Key(Text(messages("correspondenceAddress.checkYourAnswersLabel"))),
        value = Value(HtmlContent(formattedLines(address))),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = EnterYourOrganisationAddressController.onPageLoad().url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("correspondenceAddress.change.hidden"))
              )
            )
          )
        )
      )
    }

  private def hasAnyLine(address: CorrespondenceAddress): Boolean =
    Seq(address.addressLine1, address.addressLine2, address.addressLine3, address.postCode).exists(_.isDefined)

  private def formattedLines(address: CorrespondenceAddress): String =
    lines(address)
      .map(line => HtmlFormat.escape(line).toString)
      .mkString("<br>")

  def lines(address: CorrespondenceAddress): Seq[String] =
    Seq(address.addressLine1, address.addressLine2, address.addressLine3, address.postCode).flatten
}
