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
import uk.gov.hmrc.disaaccountfrontend.forms.OrganisationEmailAddressFormProvider
import utils.BaseUnitSpec

class OrganisationEmailAddressFormProviderSpec extends BaseUnitSpec {

  val form: Form[String] = new OrganisationEmailAddressFormProvider()()

  "OrganisationEmailAddressFormProvider" should {

    "bind valid email addresses" in
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

    "trim surrounding whitespace from a valid email address" in {
      val result = form.bind(Map("value" -> "  test@example.com  "))

      result.errors shouldBe empty
      result.value  shouldBe Some("test@example.com")
    }

    "return an error when the email address is missing" in {
      val result = form.bind(Map("value" -> ""))

      result.errors.map(_.message) should contain("organisationEmailAddress.error.required")
    }

    "return an error when the email address is an invalid format" in {
      val result = form.bind(Map("value" -> "not-an-email"))

      result.errors.map(_.message) should contain("organisationEmailAddress.error.format")
    }
  }
}
