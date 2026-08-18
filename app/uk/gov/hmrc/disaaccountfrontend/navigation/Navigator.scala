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

package uk.gov.hmrc.disaaccountfrontend.navigation

import play.api.mvc.Call
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.OrganisationTelephoneNumberController

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  def nextPage(page: Page): Call = page match {
    case EnterYourOrganisationAddressPage => OrganisationTelephoneNumberController.onPageLoad()
    // TODO: replace with the next page in the journey once it exists.
    case OrganisationTelephoneNumberPage  => OrganisationTelephoneNumberController.onPageLoad()
  }
}
