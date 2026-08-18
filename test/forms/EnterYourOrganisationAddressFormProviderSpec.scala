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
import uk.gov.hmrc.disaaccountfrontend.forms.EnterYourOrganisationAddressFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.CorrespondenceAddress
import utils.BaseUnitSpec

class EnterYourOrganisationAddressFormProviderSpec extends BaseUnitSpec {

  val form: Form[CorrespondenceAddress] = new EnterYourOrganisationAddressFormProvider()()

  val validData: Map[String, String] = Map(
    "addressLine1" -> "1 Test Street",
    "addressLine2" -> "Test District",
    "townOrCity"   -> "Test Town",
    "postcode"     -> "AA1 1AA"
  )

  "EnterYourOrganisationAddressFormProvider" should {

    "bind valid data" in {
      val result = form.bind(validData)

      result.errors shouldBe empty
      result.value  shouldBe Some(
        CorrespondenceAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Test District"),
          addressLine3 = Some("Test Town"),
          postCode = Some("AA1 1AA")
        )
      )
    }

    "bind valid data without the optional address line 2" in {
      val result = form.bind(validData - "addressLine2")

      result.errors shouldBe empty
    }

    "fill from a CorrespondenceAddress" in {
      val filled = form.fill(
        CorrespondenceAddress(
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Test District"),
          addressLine3 = Some("Test Town"),
          postCode = Some("AA1 1AA")
        )
      )

      filled("addressLine1").value shouldBe Some("1 Test Street")
      filled("townOrCity").value   shouldBe Some("Test Town")
      filled("postcode").value     shouldBe Some("AA1 1AA")
    }

    "return an error when address line 1 is missing" in {
      val result = form.bind(validData - "addressLine1")

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.addressLine1.required")
    }

    "return an error when address line 1 is longer than 35 characters" in {
      val result = form.bind(validData.updated("addressLine1", "a" * 36))

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.addressLine1.length")
    }

    "return an error when town or city is missing" in {
      val result = form.bind(validData - "townOrCity")

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.townOrCity.required")
    }

    "return an error when postcode is missing" in {
      val result = form.bind(validData - "postcode")

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.postcode.required")
    }

    "return an error when postcode is too short" in {
      val result = form.bind(validData.updated("postcode", "AA1"))

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.postcode.tooShort")
    }

    "return an error when postcode is too long" in {
      val result = form.bind(validData.updated("postcode", "AA1 1AA XXX"))

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.postcode.tooLong")
    }

    "return an error when postcode is an invalid format" in {
      val result = form.bind(validData.updated("postcode", "ZZZZZ"))

      result.errors.map(_.message) should contain("enterYourOrganisationAddress.error.postcode.invalid")
    }
  }
}
