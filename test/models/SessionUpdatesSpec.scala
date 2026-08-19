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

package models

import play.api.libs.json.{JsNull, JsSuccess, Json}
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates}
import utils.BaseUnitSpec

class SessionUpdatesSpec extends BaseUnitSpec {

  private val updatedOrgTelephoneNumber = "07777777777"

  "SessionUpdates" should {

    "round-trip typed updates through JSON" in {
      val sessionUpdates = SessionUpdates(
        correspondenceAddress = Assign(testCorrespondenceAddress),
        isaProducts = Assign(Seq.empty),
        p2pPlatform = Clear
      )

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "default missing fields to unchanged" in {
      Json.obj().validate[SessionUpdates] shouldBe JsSuccess(SessionUpdates())
    }

    "read legacy cached values as set and legacy nulls as unchanged" in {
      val legacyJson = Json.obj(
        "correspondenceAddress"       -> Json.toJson(testCorrespondenceAddress),
        "organisationTelephoneNumber" -> JsNull,
        "p2pPlatform"                 -> testP2pPlatform
      )

      legacyJson.validate[SessionUpdates] shouldBe JsSuccess(
        SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Unchanged,
          p2pPlatform = Assign(testP2pPlatform)
        )
      )
    }

    "apply typed updates to an answer snapshot" in {
      val answers = Answers(
        correspondenceAddress = Some(testCorrespondenceAddress),
        organisationTelephoneNumber = Some(testOrgTelephoneNumber),
        p2pPlatform = Some(testP2pPlatform)
      )
      val updates = SessionUpdates(
        organisationTelephoneNumber = Assign(updatedOrgTelephoneNumber),
        p2pPlatform = Clear
      )

      updates.getEffectiveAnswers(answers) shouldBe Answers(
        correspondenceAddress = Some(testCorrespondenceAddress),
        organisationTelephoneNumber = Some(updatedOrgTelephoneNumber)
      )
    }
  }
}
