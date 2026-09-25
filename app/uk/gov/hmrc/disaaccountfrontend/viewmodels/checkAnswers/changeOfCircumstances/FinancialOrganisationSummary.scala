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
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.FinancialOrganisationController
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryListRow, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}

object FinancialOrganisationSummary {

  def organisationName(organisation: FinancialOrganisation)(implicit messages: Messages): String =
    messages(s"financialOrganisation.${organisation.toString}")

  def row(answers: Answers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.financialOrganisation.filter(_.nonEmpty).map { organisations =>
      val lines = FinancialOrganisation.values.filter(organisations.contains).map(organisationName)

      SummaryListRow(
        key = Key(Text(messages("changeOfCircumstances.organisationDescription.label"))),
        value = Value(HtmlContent(lines.map(line => HtmlFormat.escape(line).toString).mkString("<br>"))),
        actions = Some(
          Actions(
            items = Seq(
              ActionItem(
                href = FinancialOrganisationController.onPageLoad().url,
                content = Text(messages("site.change")),
                visuallyHiddenText = Some(messages("changeOfCircumstances.organisationDescription.change.hidden"))
              )
            )
          )
        )
      )
    }
}
