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

import play.api.test.FakeRequest
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.pages.LiaisonOfficerPhoneNumberPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class LiaisonOfficerPhoneNumberPageSpec extends BaseUnitSpec {

  private val id = "target-id"

  private val targetOfficer = LiaisonOfficer(
    id = id,
    fullName = Some("Jane Smith"),
    phoneNumber = Some("07777777777"),
    communication = Set(ByEmail, ByPhone),
    email = Some("jane@example.com")
  )

  "LiaisonOfficerPhoneNumberPage" should {

    "allow access when the identified liaison officer exists and has a non-blank name" in {
      val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(targetOfficer))))

      LiaisonOfficerPhoneNumberPage(id).canBeAccessedWith(answers) shouldBe true
    }

    "deny access when the liaison officer section is missing" in {
      LiaisonOfficerPhoneNumberPage(id).canBeAccessedWith(Answers()) shouldBe false
    }

    "deny access when the identified liaison officer is missing" in {
      val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(targetOfficer.copy(id = "other-id")))))

      LiaisonOfficerPhoneNumberPage(id).canBeAccessedWith(answers) shouldBe false
    }

    "deny access when the identified liaison officer has no name" in {
      val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(targetOfficer.copy(fullName = None)))))

      LiaisonOfficerPhoneNumberPage(id).canBeAccessedWith(answers) shouldBe false
    }

    "deny access when the identified liaison officer has a blank name" in {
      val answers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(targetOfficer.copy(fullName = Some("  "))))))

      LiaisonOfficerPhoneNumberPage(id).canBeAccessedWith(answers) shouldBe false
    }

    "update the identified officer phone number and preserve existing session updates" in {
      val existingUpdates = SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      val otherOfficer    = LiaisonOfficer("other-id", Some("Other Name"), phoneNumber = Some("01642123456"))
      val officers        = LiaisonOfficers(Seq(otherOfficer, targetOfficer))
      val request         = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        Some(UserAnswers(testSessionId, existingUpdates)),
        Answers(liaisonOfficers = Some(officers))
      )

      LiaisonOfficerPhoneNumberPage(id).saveAnswerAndHandleDependents(request, "07123456789") shouldBe
        existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, targetOfficer.copy(phoneNumber = Some("07123456789"))))
          )
        )
    }
  }
}
