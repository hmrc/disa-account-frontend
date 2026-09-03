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

import uk.gov.hmrc.disaaccountfrontend.forms.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.{No, Yes}
import utils.BaseUnitSpec

class YesNoAnswerFormProviderSpec extends BaseUnitSpec {

  private val form = new YesNoAnswerFormProvider()("required.error")

  "YesNoAnswerFormProvider" should {

    "bind Yes" in {
      form.bind(Map("value" -> "yes")).value shouldBe Some(Yes)
    }

    "bind No" in {
      form.bind(Map("value" -> "no")).value shouldBe Some(No)
    }

    "return the configured required error when no answer is submitted" in {
      form.bind(Map.empty[String, String]).errors.map(_.message) should contain("required.error")
    }

    "reject an unsupported answer" in {
      form.bind(Map("value" -> "maybe")).errors.map(_.message) should contain("error.invalid")
    }
  }
}
