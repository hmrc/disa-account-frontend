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
import uk.gov.hmrc.disaaccountfrontend.forms.LiaisonOfficerEmailFormProvider
import utils.BaseUnitSpec

class LiaisonOfficerEmailFormProviderSpec extends BaseUnitSpec {

  private val form: Form[String] = new LiaisonOfficerEmailFormProvider()()

  "LiaisonOfficerEmailFormProvider" should {

    "bind email addresses accepted by the registration service validation" in
      Seq(
        "test@example.com",
        "user.name+tag@domain.co.uk",
        "user_name@sub.domain.com",
        "user-name@domain.org",
        "user%test@domain.io"
      ).foreach { email =>
        val result = form.bind(Map("value" -> email))

        result.errors shouldBe empty
        result.value  shouldBe Some(email)
      }

    "trim surrounding whitespace before validating" in {
      val result = form.bind(Map("value" -> "  liaison@example.com  "))

      result.errors shouldBe empty
      result.value  shouldBe Some("liaison@example.com")
    }

    "return the required error for a missing value" in {
      form.bind(Map.empty[String, String]).errors.map(_.message) should contain(
        "liaisonOfficerEmail.error.required"
      )
    }

    "return the required error for a whitespace-only value" in {
      form.bind(Map("value" -> "   ")).errors.map(_.message) should contain(
        "liaisonOfficerEmail.error.required"
      )
    }

    "return the invalid error for an incorrectly formatted email address" in {
      form.bind(Map("value" -> "not-an-email")).errors.map(_.message) should contain(
        "liaisonOfficerEmail.error.invalid"
      )
    }
  }
}
