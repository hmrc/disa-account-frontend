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
import uk.gov.hmrc.disaaccountfrontend.forms.SignatoryNameFormProvider
import utils.BaseUnitSpec

class SignatoryNameFormProviderSpec extends BaseUnitSpec {

  val form: Form[String] = new SignatoryNameFormProvider()()

  "SignatoryNameFormProvider" should {

    "bind a valid signatory name" in {
      val result = form.bind(Map("value" -> "Jane O'Smith-Jones"))

      result.errors shouldBe empty
      result.value  shouldBe Some("Jane O'Smith-Jones")
    }

    "fill from a signatory name" in {
      val filled = form.fill("Jane Smith")

      filled("value").value shouldBe Some("Jane Smith")
    }

    "return an error when the value is missing" in {
      val result = form.bind(Map("value" -> ""))

      result.errors.map(_.message) should contain("signatoryName.error.required")
    }

    "return an error when the value is whitespace only" in {
      val result = form.bind(Map("value" -> "   "))

      result.errors.map(_.message) should contain("signatoryName.error.required")
    }

    "return an error when the value is absent from the data" in {
      val result = form.bind(Map.empty[String, String])

      result.errors.map(_.message) should contain("signatoryName.error.required")
    }

    "return an error when the value contains characters other than letters, hyphens, spaces and apostrophes" in {
      val result = form.bind(Map("value" -> "Jane123"))

      result.errors.map(_.message) should contain("signatoryName.error.invalid")
    }
  }
}
