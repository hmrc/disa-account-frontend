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
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.{ChangeOfCircumstancesController, PeerToPeerPlatformController}
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.{OrganisationTelephoneNumberController, TradingNameController}
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.pages.{EnterYourOrganisationAddressPage, InnovativeFinancialProductsPage, OrganisationTelephoneNumberPage, Page, PeerToPeerPlatformPage, TradingNamePage}

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  def nextPage(page: Page, answers: Answers = Answers()): Call = page match {
    case EnterYourOrganisationAddressPage => OrganisationTelephoneNumberController.onPageLoad()
    // TODO: replace with the next page in the journey once it exists.
    case OrganisationTelephoneNumberPage  => OrganisationTelephoneNumberController.onPageLoad()
    case TradingNamePage                  => TradingNameController.onPageLoad()
    case InnovativeFinancialProductsPage  => innovativeFinancialProductsNextPage(answers)
    case PeerToPeerPlatformPage           => peerToPeerPlatformNextPage(answers)
    case unsupportedPage                  =>
      throw new IllegalArgumentException(s"No navigation defined for page: $unsupportedPage")
  }

  private def innovativeFinancialProductsNextPage(answers: Answers): Call =
    answers.innovativeFinancialProducts match {
      case Some(products) if products.contains(PeertopeerLoansUsingAPlatformWith36hPermissions) =>
        peerToPeerPlatformQuestionPage
      case _                                                                                    =>
        ChangeOfCircumstancesController.onPageLoad()
    }

  private def peerToPeerPlatformQuestionPage: Call =
    PeerToPeerPlatformController.onPageLoad()

  private def peerToPeerPlatformNextPage(answers: Answers): Call =
    answers.p2pPlatformNumber match {
      case Some(_) => ChangeOfCircumstancesController.onPageLoad()
      case None    => peerToPeerPlatformNumberQuestionPage
    }

  private def peerToPeerPlatformNumberQuestionPage: Call =
    // TODO: Replace this fallback with the FCA/FRN question when that page is implemented.
    ChangeOfCircumstancesController.onPageLoad()
}
