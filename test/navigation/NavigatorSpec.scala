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

import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.OrganisationTelephoneNumberController
import uk.gov.hmrc.disaaccountfrontend.navigation.{EnterYourOrganisationAddressPage, Navigator, OrganisationTelephoneNumberPage}
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
  }
}
