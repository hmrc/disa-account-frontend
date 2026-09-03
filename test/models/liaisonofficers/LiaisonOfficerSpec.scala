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

package models.liaisonofficers

import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.ByEmail
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficer
import utils.BaseUnitSpec

class LiaisonOfficerSpec extends BaseUnitSpec {

  private val completeOfficer = LiaisonOfficer(
    id = "id",
    fullName = Some("Jane Smith"),
    phoneNumber = Some("07777777777"),
    communication = Set(ByEmail),
    email = Some("jane.smith@example.com")
  )

  "LiaisonOfficer.isComplete" should {

    "return true when all required details are present" in {
      completeOfficer.isComplete shouldBe true
    }

    "return false when a required detail is absent" in {
      completeOfficer.copy(fullName = None).isComplete           shouldBe false
      completeOfficer.copy(phoneNumber = None).isComplete        shouldBe false
      completeOfficer.copy(communication = Set.empty).isComplete shouldBe false
      completeOfficer.copy(email = None).isComplete              shouldBe false
    }
  }
}
