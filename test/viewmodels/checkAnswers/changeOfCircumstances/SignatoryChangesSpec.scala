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

package viewmodels.checkAnswers.changeOfCircumstances

import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances.SignatoryChanges
import utils.BaseUnitSpec

class SignatoryChangesSpec extends BaseUnitSpec {

  "SignatoryChanges" should {

    "have no changes when nothing changed" in {
      val answers = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))

      val changes = SignatoryChanges(answers, answers)

      changes.hasChanges shouldBe false
    }

    "list a signatory as added when its id is new" in {
      val original  = Answers(signatories = None)
      val effective = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))

      val changes = SignatoryChanges(original, effective)

      changes.added      shouldBe Seq("Jane Doe")
      changes.removed     shouldBe Seq.empty
      changes.hasChanges shouldBe true
    }

    "list a signatory as removed when its id is no longer present" in {
      val original  = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))
      val effective = Answers(signatories = None)

      val changes = SignatoryChanges(original, effective)

      changes.added   shouldBe Seq.empty
      changes.removed shouldBe Seq("Jane Doe")
    }

    "not treat an edited signatory as added or removed" in {
      val original  = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))
      val effective = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("CEO"))))))

      val changes = SignatoryChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "ignore incomplete signatories" in {
      val original  = Answers(signatories = None)
      val effective = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), None)))))

      val changes = SignatoryChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "not treat a signatory as changed when only its id differs but the name matches" in {
      val original  = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Jane Doe"), Some("Director"))))))
      val effective = Answers(signatories = Some(Signatories(Seq(Signatory("s-2", Some("Jane Doe"), Some("Director"))))))

      val changes = SignatoryChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "still detect a genuine add alongside a signatory whose id changed but name matches" in {
      val original  = Answers(signatories = Some(Signatories(Seq(Signatory("s-1", Some("Test Signatory"), Some("Director"))))))
      val effective = Answers(
        signatories = Some(
          Signatories(
            Seq(
              Signatory("s-2", Some("Test Signatory"), Some("Director")),
              Signatory("s-3", Some("Zach"), Some("Director"))
            )
          )
        )
      )

      val changes = SignatoryChanges(original, effective)

      changes.added   shouldBe Seq("Zach")
      changes.removed shouldBe Seq.empty
    }
  }
}
