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

package models.certificatesofauthority

import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import utils.BaseUnitSpec

class FinancialOrganisationSpec extends BaseUnitSpec {

  "FinancialOrganisation" should {

    "serialise and deserialise every supported wire value" in
      FinancialOrganisation.values.foreach { organisation =>
        Json.toJson(organisation)                                             shouldBe JsString(organisation.toString)
        JsString(organisation.toString).validate[FinancialOrganisation].asOpt shouldBe Some(organisation)
      }

    "reject an unsupported wire value" in {
      JsString("unsupported").validate[FinancialOrganisation] shouldBe JsError("error.invalid")
    }
  }
}
