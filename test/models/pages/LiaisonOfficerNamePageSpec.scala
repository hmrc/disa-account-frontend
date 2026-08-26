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
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.pages.LiaisonOfficerNamePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class LiaisonOfficerNamePageSpec extends BaseUnitSpec {

  "LiaisonOfficerNamePage" should {

    "upsert the identified officer and preserve existing session updates" in {
      val existingUpdates = SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      val otherOfficer    = LiaisonOfficer("other-id", Some("Other Name"))
      val targetOfficer   = LiaisonOfficer("target-id", Some("Old Name"), phoneNumber = Some("07777777777"))
      val request         = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        Some(UserAnswers(testSessionId, existingUpdates)),
        Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(otherOfficer, targetOfficer))))
      )

      LiaisonOfficerNamePage("target-id").saveAnswerAndHandleDependents(request, "Updated Name") shouldBe
        existingUpdates.copy(
          liaisonOfficers = Assign(
            LiaisonOfficers(Seq(otherOfficer, targetOfficer.copy(fullName = Some("Updated Name"))))
          )
        )
    }
  }
}
