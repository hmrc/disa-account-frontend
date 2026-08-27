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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.{ByEmail, ByPhone}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import utils.BaseUnitSpec

class LiaisonOfficersSpec extends BaseUnitSpec {

  private val existingOfficer = LiaisonOfficer(
    id = "existing-id",
    fullName = Some("Old Name"),
    phoneNumber = Some("07777777777"),
    communication = Set(ByEmail, ByPhone),
    email = Some("old.name@example.com")
  )

  "LiaisonOfficers" should {

    "round-trip the registration-compatible model through JSON" in {
      val officers = LiaisonOfficers(Seq(existingOfficer))

      Json.toJson(officers).validate[LiaisonOfficers] shouldBe JsSuccess(officers)
    }

    "update the matching name without losing other details or officers" in {
      val other    = LiaisonOfficer("other-id", Some("Other Name"))
      val officers = LiaisonOfficers(Seq(other, existingOfficer))

      officers.upsertName(existingOfficer.id, "Updated Name") shouldBe LiaisonOfficers(
        Seq(other, existingOfficer.copy(fullName = Some("Updated Name")))
      )
    }

    "append a new liaison officer when the id is not present" in {
      LiaisonOfficers(Seq(existingOfficer)).upsertName("new-id", "New Name") shouldBe LiaisonOfficers(
        Seq(existingOfficer, LiaisonOfficer("new-id", Some("New Name")))
      )
    }

    "update the matching email without losing other details, officers or ordering" in {
      val other    = LiaisonOfficer("other-id", Some("Other Name"), email = Some("other@example.com"))
      val officers = LiaisonOfficers(Seq(other, existingOfficer))

      officers.updateEmail(existingOfficer.id, "updated@example.com") shouldBe LiaisonOfficers(
        Seq(other, existingOfficer.copy(email = Some("updated@example.com")))
      )
    }

    "leave the officers unchanged when updating an unknown id" in {
      val officers = LiaisonOfficers(Seq(existingOfficer))

      officers.updateEmail("unknown-id", "updated@example.com") shouldBe officers
    }
  }
}
