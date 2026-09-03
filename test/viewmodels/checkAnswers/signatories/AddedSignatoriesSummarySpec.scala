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

package viewmodels.checkAnswers.signatories

import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.signatories.AddedSignatoriesSummary
import utils.BaseUnitSpec

class AddedSignatoriesSummarySpec extends BaseUnitSpec {

  private def signatory(number: Int): Signatory =
    Signatory(s"signatory-$number", Some(s"Signatory $number"), Some("Director"))

  "AddedSignatoriesSummary" should {

    "use the singular heading for one signatory" in {
      val summary = AddedSignatoriesSummary(Seq(signatory(1)), 25)

      summary.title(messages(app)) shouldBe "You currently have a signatory"
      summary.canAddMore          shouldBe true
    }

    "use the plural heading and maximum state above the configured maximum" in {
      val summary = AddedSignatoriesSummary((1 to 26).map(signatory), 25)

      summary.title(messages(app))    shouldBe "You have 26 signatories"
      summary.guidance(messages(app)) shouldBe "You must have at least one signatory. The maximum is 25."
      summary.canAddMore              shouldBe false
    }
  }
}
