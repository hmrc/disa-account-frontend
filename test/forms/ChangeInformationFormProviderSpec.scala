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

package forms

import play.api.data.Form
import uk.gov.hmrc.disaaccountfrontend.forms.ChangeInformationFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection
import utils.BaseUnitSpec

class ChangeInformationFormProviderSpec extends BaseUnitSpec {

  val form: Form[Set[String]] = new ChangeInformationFormProvider()(ChangeInformationSelection.values)

  "ChangeInformationFormProvider" should {

    "bind every supported selection" in
      ChangeInformationSelection.validFormValues(ChangeInformationSelection.values).foreach { selection =>
        val result = form.bind(Map("value[0]" -> selection))

        result.errors shouldBe empty
        result.value  shouldBe Some(Set(selection))
      }

    "bind multiple selections" in {
      val selected = ChangeInformationSelection.values.take(2).map(_.toString)
      val result   = form.bind(selected.zipWithIndex.map { case (selection, index) =>
        s"value[$index]" -> selection
      }.toMap)

      result.errors shouldBe empty
      result.value  shouldBe Some(selected.toSet)
    }

    "return the required error when no selection is made" in {
      val result = form.bind(Map.empty[String, String])

      result.errors.map(_.message) should contain("changeInformation.error.required")
    }

    "reject an unsupported selection" in {
      val result = form.bind(Map("value[0]" -> "unsupported"))

      result.errors.map(_.message) should contain("changeInformation.error.required")
    }

    "reject ISA product information when it is unavailable" in {
      val restrictedForm = new ChangeInformationFormProvider()(
        ChangeInformationSelection.availableValues(isSignatory = false)
      )
      val result         = restrictedForm.bind(Map("value[0]" -> "isaProductInformation"))

      result.errors.map(_.message) should contain("changeInformation.error.required")
    }

    "fill previously selected values" in {
      val selected = ChangeInformationSelection.values.takeRight(2).map(_.toString).toSet
      val filled   = form.fill(selected)

      filled.value             shouldBe Some(selected)
      filled.data.values.toSet shouldBe selected
    }
  }
}
