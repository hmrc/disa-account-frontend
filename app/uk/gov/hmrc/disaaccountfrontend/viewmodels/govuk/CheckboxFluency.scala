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

package uk.gov.hmrc.disaaccountfrontend.viewmodels.govuk

import play.api.data.Form
import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.viewmodels.ErrorMessageAwareness
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.{CheckboxItem, Checkboxes}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Content
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.Legend
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint

object checkbox extends CheckboxFluency

trait CheckboxFluency extends FieldsetFluency {

  object CheckboxesViewModel extends ErrorMessageAwareness {

    def apply(
      form: Form[_],
      name: String,
      items: Seq[CheckboxItem],
      legend: Legend,
      hint: Option[Hint]
    )(implicit messages: Messages): Checkboxes =
      Checkboxes(
        fieldset = Some(FieldsetViewModel(legend)),
        name = name,
        hint = hint,
        errorMessage = errorMessage(form(name)),
        items = items.map(item => item.copy(checked = form.data.exists(_._2 == item.value)))
      )
  }

  object CheckboxItemViewModel {

    def apply(content: Content, fieldId: String, index: Int, value: String): CheckboxItem =
      CheckboxItem(
        content = content,
        id = Some(s"${fieldId}_$index"),
        name = Some(s"$fieldId[$index]"),
        value = value
      )
  }
}
