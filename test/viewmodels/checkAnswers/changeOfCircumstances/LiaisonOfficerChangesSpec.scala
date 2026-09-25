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
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances.LiaisonOfficerChanges
import utils.BaseUnitSpec

class LiaisonOfficerChangesSpec extends BaseUnitSpec {

  private def officer(id: String, name: String): LiaisonOfficer =
    LiaisonOfficer(id, Some(name), Some("0123"), Set(LiaisonOfficerCommunication.values.head), Some(s"$id@example.com"))

  "LiaisonOfficerChanges" should {

    "have no changes when nothing changed" in {
      val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith")))))

      val changes = LiaisonOfficerChanges(answers, answers)

      changes.hasChanges shouldBe false
    }

    "list a liaison officer as added when its id is new" in {
      val original  = Answers(liaisonOfficers = None)
      val effective = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith")))))

      val changes = LiaisonOfficerChanges(original, effective)

      changes.added      shouldBe Seq("John Smith")
      changes.removed    shouldBe Seq.empty
      changes.hasChanges shouldBe true
    }

    "list a liaison officer as removed when its id is no longer present" in {
      val original  = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith")))))
      val effective = Answers(liaisonOfficers = None)

      val changes = LiaisonOfficerChanges(original, effective)

      changes.added   shouldBe Seq.empty
      changes.removed shouldBe Seq("John Smith")
    }

    "not treat an edited liaison officer as added or removed" in {
      val original  = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith")))))
      val effective = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith").copy(phoneNumber = Some("0999")))))
      )

      val changes = LiaisonOfficerChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "ignore incomplete liaison officers" in {
      val original  = Answers(liaisonOfficers = None)
      val effective = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(LiaisonOfficer("lo-1", Some("John Smith")))))
      )

      val changes = LiaisonOfficerChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "not treat a liaison officer as changed when only its id differs but the name matches" in {
      val original  = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "John Smith")))))
      val effective = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-2", "John Smith")))))

      val changes = LiaisonOfficerChanges(original, effective)

      changes.hasChanges shouldBe false
    }

    "still detect a genuine add alongside a liaison officer whose id changed but name matches" in {
      val original  = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-1", "Test Officer")))))
      val effective = Answers(
        liaisonOfficers = Some(LiaisonOfficers(Seq(officer("lo-2", "Test Officer"), officer("lo-3", "Zach"))))
      )

      val changes = LiaisonOfficerChanges(original, effective)

      changes.added   shouldBe Seq("Zach")
      changes.removed shouldBe Seq.empty
    }
  }
}
