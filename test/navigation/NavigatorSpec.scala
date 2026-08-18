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

package navigation

import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.OrganisationTelephoneNumberController
import uk.gov.hmrc.disaaccountfrontend.models.SessionUpdates
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansUsingAPlatformWith36hPermissions}
import uk.gov.hmrc.disaaccountfrontend.navigation.{EnterYourOrganisationAddressPage, InnovativeFinancialProductsPage, Navigator, OrganisationTelephoneNumberPage}
import utils.BaseUnitSpec

class NavigatorSpec extends BaseUnitSpec {

  val navigator = new Navigator()

  "Navigator" should {

    "go from EnterYourOrganisationAddressPage to the organisation telephone number page" in {
      navigator.nextPage(EnterYourOrganisationAddressPage) shouldBe OrganisationTelephoneNumberController.onPageLoad()
    }

    "go from OrganisationTelephoneNumberPage back to itself until the next page in the journey exists" in {
      navigator.nextPage(OrganisationTelephoneNumberPage) shouldBe OrganisationTelephoneNumberController.onPageLoad()
    }

    "temporarily go from InnovativeFinancialProductsPage to change of circumstances when the platform option is selected" in {
      val answers = SessionUpdates(
        innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions))
      )

      navigator.nextPage(InnovativeFinancialProductsPage, answers) shouldBe
        ChangeOfCircumstancesController.onPageLoad()
    }

    "go from InnovativeFinancialProductsPage to change of circumstances when the platform option is not selected" in {
      val answers = SessionUpdates(innovativeFinancialProducts = Some(Seq(CrowdFundedDebentures)))

      navigator.nextPage(InnovativeFinancialProductsPage, answers) shouldBe
        ChangeOfCircumstancesController.onPageLoad()
    }
  }
}
