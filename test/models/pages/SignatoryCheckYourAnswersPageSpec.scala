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
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryCheckYourAnswersPage
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import utils.BaseUnitSpec

class SignatoryCheckYourAnswersPageSpec extends BaseUnitSpec {

  private val page = SignatoryCheckYourAnswersPage(testSignatoryId)

  private val completeSignatory =
    Signatory(testSignatoryId, Some(testSignatoryName), Some(testSignatoryJobTitle))

  "SignatoryCheckYourAnswersPage" should {

    "allow access with complete matching signatory details" in {
      page.canBeAccessedWith(Answers(signatories = Some(Seq(completeSignatory)))) shouldBe true
    }

    "deny access when the signatory is missing" in {
      page.canBeAccessedWith(Answers(signatories = Some(Seq.empty))) shouldBe false
    }

    "deny access when the signatory name is missing" in {
      page.canBeAccessedWith(
        Answers(signatories = Some(Seq(completeSignatory.copy(fullName = None))))
      ) shouldBe false
    }

    "deny access when the signatory job title is missing" in {
      page.canBeAccessedWith(
        Answers(signatories = Some(Seq(completeSignatory.copy(jobTitle = None))))
      ) shouldBe false
    }
  }
}
