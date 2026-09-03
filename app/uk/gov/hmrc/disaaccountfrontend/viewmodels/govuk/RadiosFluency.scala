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

import play.api.data.Field
import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.viewmodels.ErrorMessageAwareness
import uk.gov.hmrc.govukfrontend.views.viewmodels.fieldset.{Fieldset, Legend}
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.{RadioItem, Radios}

trait RadiosFluency {

  object RadiosViewModel extends ErrorMessageAwareness with FieldsetFluency {

    def apply(field: Field, items: Seq[RadioItem], legend: Legend)(implicit messages: Messages): Radios =
      Radios(
        fieldset = Some(FieldsetViewModel(legend)),
        name = field.name,
        items = items.map(item => item.copy(checked = field.value.isDefined && field.value == item.value)),
        errorMessage = errorMessage(field)
      )
  }

  implicit class FluentRadios(radios: Radios) {

    def inline(): Radios =
      radios.copy(classes = s"${radios.classes} govuk-radios--inline")
  }
}
