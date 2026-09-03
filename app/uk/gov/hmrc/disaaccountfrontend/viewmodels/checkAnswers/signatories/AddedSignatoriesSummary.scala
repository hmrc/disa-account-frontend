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
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.controllers.signatories.routes.SignatoryCheckYourAnswersController
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Text, Value}

case class AddedSignatoriesSummary(signatories: Seq[Signatory], maxSignatories: Int) {

  private val completeSignatories = signatories.filter(_.isComplete)

  val count: Int          = completeSignatories.size
  val canAddMore: Boolean = count < maxSignatories

  def title(implicit messages: Messages): String =
    if (count == 1) messages("addedSignatory.title")
    else messages("addedSignatory.title.plural", count)

  def guidance(implicit messages: Messages): String =
    if (canAddMore) messages("addedSignatory.guidance", maxSignatories)
    else messages("addedSignatory.guidance.max", maxSignatories)

  def list(implicit messages: Messages): SummaryList =
    SummaryList(rows = completeSignatories.flatMap(row))

  private def row(signatory: Signatory)(implicit messages: Messages): Option[SummaryListRow] =
    signatory.fullName.map { name =>
      SummaryListRow(
        key = Key(Text(name), classes = "govuk-!-font-weight-regular"),
        value = Value(Text(""), classes = "govuk-!-width-one-quarter"),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = SignatoryCheckYourAnswersController.onPageLoad(signatory.id).url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("addedSignatory.summary.action.hidden", name))
              ),
              ActionItem(
                // TODO: Replace this fallback with the remove-signatory page once it is implemented.
                href = ChangeOfCircumstancesController.onPageLoad().url,
                content = Text(messages("site.remove")),
                visuallyHiddenText = Some(messages("addedSignatory.summary.action.hidden", name))
              )
            )
          )
        )
      )
    }
}
