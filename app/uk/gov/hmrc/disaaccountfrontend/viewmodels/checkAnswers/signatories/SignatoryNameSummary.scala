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

package uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.signatories

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.controllers.signatories.routes.SignatoryNameController
import uk.gov.hmrc.disaaccountfrontend.models.CheckMode
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

object SignatoryNameSummary {

  def row(signatory: Signatory)(implicit messages: Messages): Option[SummaryListRow] =
    signatory.fullName.map { name =>
      SummaryListRow(
        key = Key(Text(messages("signatoryName.checkYourAnswersLabel"))),
        value = Value(Text(name)),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = SignatoryNameController.onPageLoad(Some(signatory.id), CheckMode).url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("signatoryName.change.hidden"))
              )
            )
          )
        )
      )
    }
}
