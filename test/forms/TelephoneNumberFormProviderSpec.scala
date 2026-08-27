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
import uk.gov.hmrc.disaaccountfrontend.forms.TelephoneNumberFormProvider
import utils.BaseUnitSpec

class TelephoneNumberFormProviderSpec extends BaseUnitSpec {

  val keyPrefix: String  = "organisationTelephoneNumber"
  val form: Form[String] = new TelephoneNumberFormProvider()(keyPrefix)

  "TelephoneNumberFormProvider" should {

    "bind a valid 11 digit phone number" in {
      val result = form.bind(Map("value" -> "01642123456"))

      result.errors shouldBe empty
      result.value  shouldBe Some("01642123456")
    }

    "strip whitespace before validating and binding" in {
      val result = form.bind(Map("value" -> "01642 123 456"))

      result.errors shouldBe empty
      result.value  shouldBe Some("01642123456")
    }

    "fill from a phone number" in {
      val filled = form.fill("01642123456")

      filled("value").value shouldBe Some("01642123456")
    }

    "return an error when the value is missing" in {
      val result = form.bind(Map("value" -> ""))

      result.errors.map(_.message) should contain(s"$keyPrefix.error.required")
    }

    "return an error when the value contains invalid characters" in
      Seq("0164212345a", "01642-123456", "01642'123456").foreach { phoneNumber =>
        val result = form.bind(Map("value" -> phoneNumber))

        result.errors.map(_.message) should contain(s"$keyPrefix.error.invalid")
      }

    "return an error when the value is too short" in {
      val result = form.bind(Map("value" -> "0164212345"))

      result.errors.map(_.message) should contain(s"$keyPrefix.error.tooShort")
    }

    "return an error when the value is too long" in {
      val result = form.bind(Map("value" -> "016421234567"))

      result.errors.map(_.message) should contain(s"$keyPrefix.error.tooLong")
    }
  }
}
