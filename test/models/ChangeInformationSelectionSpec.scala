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

import play.api.libs.json.{JsError, JsString, Json}
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.*
import utils.BaseUnitSpec

class ChangeInformationSelectionSpec extends BaseUnitSpec {

  "ChangeInformationSelection" should {

    "serialise and deserialise every supported wire value" in
      values.foreach { selection =>
        Json.toJson(selection)                                             shouldBe JsString(selection.toString)
        JsString(selection.toString).validate[ChangeInformationSelection].asOpt shouldBe Some(selection)
      }

    "reject an unsupported wire value" in {
      JsString("unsupported").validate[ChangeInformationSelection] shouldBe JsError("error.invalid")
    }

    "convert submitted values to selections in display order" in {
      val availableSelections = availableValues(isSignatory = true)

      fromForm(Set(AuthorisedUsers.toString, OrganisationInformation.toString), availableSelections) shouldBe Seq(
        OrganisationInformation,
        AuthorisedUsers
      )
    }

    "preserve view all information as the submitted selection" in {
      fromForm(Set(viewAllInformationFormValue), availableValues(isSignatory = true)) shouldBe Seq(ViewAllInformation)
    }

    "give view all information precedence over other submitted values" in {
      fromForm(
        Set(viewAllInformationFormValue, OrganisationInformation.toString),
        availableValues(isSignatory = true)
      ) shouldBe Seq(ViewAllInformation)
    }

    "expose only supported checkbox form values" in {
      validFormValues(values) shouldBe values.map(_.toString).toSet + viewAllInformationFormValue
    }

    "exclude ISA product information when the user is not a signatory" in {
      availableValues(isSignatory = false) shouldBe Seq(OrganisationInformation, AuthorisedUsers)
    }

    "preserve view all information for a non-signatory" in {
      val availableSelections = availableValues(isSignatory = false)

      fromForm(Set(viewAllInformationFormValue), availableSelections) shouldBe Seq(ViewAllInformation)
    }
  }
}
