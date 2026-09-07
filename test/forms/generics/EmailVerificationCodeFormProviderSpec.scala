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

package forms.generics

import play.api.data.Form
import uk.gov.hmrc.disaaccountfrontend.forms.generic.EmailVerificationCodeFormProvider
import utils.BaseUnitSpec

class EmailVerificationCodeFormProviderSpec extends BaseUnitSpec {

  val form: Form[String] = new EmailVerificationCodeFormProvider()()

  "EmailVerificationCodeFormProvider" should {

    "bind a valid 6 letter code" in {
      val result = form.bind(Map("value" -> "ABCDEF"))

      result.errors shouldBe empty
      result.value  shouldBe Some("ABCDEF")
    }

    "trim and upper-case the code" in {
      val result = form.bind(Map("value" -> "  abcdef  "))

      result.errors shouldBe empty
      result.value  shouldBe Some("ABCDEF")
    }

    "return an error when the code is missing" in {
      val result = form.bind(Map("value" -> ""))

      result.errors.map(_.message) should contain("emailVerificationCode.error.required")
    }

    "return an error when the code contains non-letter characters" in {
      val result = form.bind(Map("value" -> "ABC123"))

      result.errors.map(_.message) should contain("emailVerificationCode.error.format")
    }

    "return an error when the code is too short" in {
      val result = form.bind(Map("value" -> "ABC"))

      result.errors.map(_.message) should contain("emailVerificationCode.error.tooShort")
    }

    "return an error when the code is too long" in {
      val result = form.bind(Map("value" -> "ABCDEFGH"))

      result.errors.map(_.message) should contain("emailVerificationCode.error.tooLong")
    }
  }
}
