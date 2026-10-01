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
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.TradingNameController
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

object TradingNameSummary {

  def row(answers: Answers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.tradingName.map { tradingName =>
      SummaryListRow(
        key = Key(Text(messages("tradingName.checkYourAnswersLabel"))),
        value = Value(Text(tradingName)),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = TradingNameController.onPageLoad().url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("tradingName.change.hidden"))
              )
            )
          )
        )
      )
    }
}
