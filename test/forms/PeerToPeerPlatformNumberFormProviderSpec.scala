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
import uk.gov.hmrc.disaaccountfrontend.forms.PeerToPeerPlatformNumberFormProvider
import utils.BaseUnitSpec

class PeerToPeerPlatformNumberFormProviderSpec extends BaseUnitSpec {

  private val formProvider              = new PeerToPeerPlatformNumberFormProvider()
  private def form: Form[String]        = formProvider(testP2pPlatform)

  private val requiredErrorMessage           = s"Enter the FCA number of $testP2pPlatform"
  private val invalidCharactersErrorMessage  =
    "The FCA must not include letters a to z, hyphens, spaces or apostrophes"
  private val patternErrorMessage            =
    "The FCA should be 6 or 7 digits without letters, hyphens, spaces or other characters"

  "PeerToPeerPlatformNumberFormProvider" should {

    "bind a valid 6 digit number" in {
      form.bind(Map("value" -> "123456")).value shouldBe Some("123456")
    }

    "bind a valid 7 digit number" in {
      form.bind(Map("value" -> "1234567")).value shouldBe Some("1234567")
    }

    "return the required error, including the platform name, for a missing value" in {
      form.bind(Map.empty[String, String]).errors.map(_.message) should contain(requiredErrorMessage)
    }

    "return the required error for a whitespace-only value" in {
      form.bind(Map("value" -> "   ")).errors.map(_.message) should contain(requiredErrorMessage)
    }

    "return the invalid characters error for a value containing a letter" in {
      form.bind(Map("value" -> "12A4567")).errors.map(_.message) should contain(invalidCharactersErrorMessage)
    }

    "return the invalid characters error for a value containing a hyphen, space or apostrophe" in {
      Seq("123-4567", "123 4567", "123'4567").foreach { value =>
        form.bind(Map("value" -> value)).errors.map(_.message) should contain(invalidCharactersErrorMessage)
      }
    }

    "return the pattern error for a value that is too short" in {
      form.bind(Map("value" -> "12345")).errors.map(_.message) should contain(patternErrorMessage)
    }

    "return the pattern error for a value that is too long" in {
      form.bind(Map("value" -> "12345678")).errors.map(_.message) should contain(patternErrorMessage)
    }

    "return the pattern error for a value containing a character that is not a letter, hyphen, space or apostrophe" in {
      form.bind(Map("value" -> "123456!")).errors.map(_.message) should contain(patternErrorMessage)
    }

    "return only the invalid characters error, and not the pattern error, when a value fails both checks" in {
      form.bind(Map("value" -> "abc")).errors.map(_.message) should contain only invalidCharactersErrorMessage
    }
  }
}
