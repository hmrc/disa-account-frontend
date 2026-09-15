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

package uk.gov.hmrc.disaaccountfrontend.forms

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, set}
import uk.gov.hmrc.disaaccountfrontend.forms.mappings.Mappings
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection

import javax.inject.Inject

class ChangeInformationFormProvider @Inject() extends Mappings {

  def apply(availableSelections: Seq[ChangeInformationSelection]): Form[Set[String]] = {
    val validFormValues = ChangeInformationSelection.validFormValues(availableSelections)

    Form(
      "value" -> set(nonEmptyText)
        .verifying(nonEmptySet("changeInformation.error.required"))
        .verifying(
          "changeInformation.error.required",
          selections => selections.forall(validFormValues.contains)
        )
    )
  }
}
