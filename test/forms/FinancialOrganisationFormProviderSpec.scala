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
import uk.gov.hmrc.disaaccountfrontend.forms.FinancialOrganisationFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import utils.BaseUnitSpec

class FinancialOrganisationFormProviderSpec extends BaseUnitSpec {

  val form: Form[Set[FinancialOrganisation]] = new FinancialOrganisationFormProvider()()

  "FinancialOrganisationFormProvider" should {

    "bind every supported organisation type" in
      FinancialOrganisation.values.zipWithIndex.foreach { case (organisation, index) =>
        val result = form.bind(Map(s"value[$index]" -> organisation.toString))

        result.errors shouldBe empty
        result.value  shouldBe Some(Set(organisation))
      }

    "bind multiple organisation types" in {
      val selected = FinancialOrganisation.values.take(2)
      val result   = form.bind(selected.zipWithIndex.map { case (organisation, index) =>
        s"value[$index]" -> organisation.toString
      }.toMap)

      result.errors shouldBe empty
      result.value  shouldBe Some(selected.toSet)
    }

    "return the required error when no organisation type is selected" in {
      val result = form.bind(Map.empty[String, String])

      result.errors.map(_.message) should contain("financialOrganisation.error.required")
    }

    "reject an unsupported organisation type" in {
      val result = form.bind(Map("value[0]" -> "unsupported"))

      result.errors.map(_.message) should contain("error.invalid")
    }

    "fill previously selected organisation types" in {
      val selected = FinancialOrganisation.values.takeRight(2).toSet
      val filled   = form.fill(selected)

      filled.value             shouldBe Some(selected)
      filled.data.values.toSet shouldBe selected.map(_.toString)
    }
  }
}
