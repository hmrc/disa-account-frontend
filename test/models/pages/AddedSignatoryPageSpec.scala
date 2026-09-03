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

package models.pages

import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.pages.AddedSignatoryPage
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import utils.BaseUnitSpec

class AddedSignatoryPageSpec extends BaseUnitSpec {

  private val completeSignatory =
    Signatory(testSignatoryId, Some(testSignatoryName), Some(testSignatoryJobTitle))

  "AddedSignatoryPage" should {

    "allow access when at least one complete signatory exists" in {
      AddedSignatoryPage.canBeAccessedWith(Answers(signatories = Some(Seq(completeSignatory)))) shouldBe true
    }

    "deny access when signatories are absent or empty" in {
      AddedSignatoryPage.canBeAccessedWith(Answers()) shouldBe false
      AddedSignatoryPage.canBeAccessedWith(Answers(signatories = Some(Seq.empty))) shouldBe false
    }

    "deny access when any signatory has incomplete details" in {
      AddedSignatoryPage.canBeAccessedWith(
        Answers(signatories = Some(Seq(completeSignatory.copy(fullName = None))))
      ) shouldBe false
      AddedSignatoryPage.canBeAccessedWith(
        Answers(signatories = Some(Seq(completeSignatory.copy(jobTitle = None))))
      ) shouldBe false
    }
  }
}
