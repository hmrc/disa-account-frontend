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
import uk.gov.hmrc.disaaccountfrontend.forms.LiaisonOfficerNameFormProvider
import utils.BaseUnitSpec

class LiaisonOfficerNameFormProviderSpec extends BaseUnitSpec {

  val form: Form[String] = new LiaisonOfficerNameFormProvider()()

  "LiaisonOfficerNameFormProvider" should {

    "bind and trim a valid name" in {
      val result = form.bind(Map("value" -> "  Élodie O’Connor-Smith  "))

      result.errors shouldBe empty
      result.value  shouldBe Some("Élodie O’Connor-Smith")
    }

    "accept straight apostrophes" in {
      form.bind(Map("value" -> "Shaun O'Connor")).errors shouldBe empty
    }

    "return the required error for a missing value" in {
      form.bind(Map.empty[String, String]).errors.map(_.message) should contain(
        "liaisonOfficerName.error.required"
      )
    }

    "return the required error for a whitespace-only value" in {
      form.bind(Map("value" -> "   ")).errors.map(_.message) should contain(
        "liaisonOfficerName.error.required"
      )
    }

    "return the invalid error for characters outside the registration name pattern" in {
      form.bind(Map("value" -> "Jane Smith 2")).errors.map(_.message) should contain(
        "liaisonOfficerName.error.invalid"
      )
    }
  }
}
