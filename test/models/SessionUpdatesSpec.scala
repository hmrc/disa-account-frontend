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

import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.CrowdFundedDebentures
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.SessionUpdates
import utils.BaseUnitSpec

class SessionUpdatesSpec extends BaseUnitSpec {

  "SessionUpdates" should {

    "round-trip through JSON when the correspondence address is present" in {
      val sessionUpdates = SessionUpdates(correspondenceAddress = Some(testCorrespondenceAddress))

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "round-trip through JSON when there is nothing saved yet" in {
      val sessionUpdates = SessionUpdates()

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "round-trip through JSON with ISA product updates" in {
      val sessionUpdates = SessionUpdates(
        isaProducts = Some(Seq(InnovativeFinanceIsas)),
        innovativeFinancialProducts = Some(Seq(CrowdFundedDebentures))
      )

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "default correspondenceAddress to None when absent from the JSON" in {
      Json.obj().validate[SessionUpdates] shouldBe JsSuccess(SessionUpdates(correspondenceAddress = None))
    }

    "round-trip through JSON when the organisation telephone number is present" in {
      val sessionUpdates = SessionUpdates(organisationTelephoneNumber = Some("01642123456"))

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "round-trip through JSON when peer-to-peer platform answers are present" in {
      val sessionUpdates = SessionUpdates(
        p2pPlatform = Some(testP2pPlatform),
        p2pPlatformNumber = Some(testP2pPlatformNumber)
      )

      Json.toJson(sessionUpdates).validate[SessionUpdates] shouldBe JsSuccess(sessionUpdates)
    }

    "default organisationTelephoneNumber to None when absent from the JSON" in {
      Json.obj().validate[SessionUpdates] shouldBe JsSuccess(SessionUpdates(organisationTelephoneNumber = None))
    }

    "default peer-to-peer platform answers to None when absent from the JSON" in {
      Json.obj().validate[SessionUpdates] shouldBe JsSuccess(
        SessionUpdates(p2pPlatform = None, p2pPlatformNumber = None)
      )
    }
  }
}
