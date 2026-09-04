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
import uk.gov.hmrc.disaaccountfrontend.controllers.liaisonofficers.routes.{LiaisonOfficerCommunicationController, LiaisonOfficerEmailController, LiaisonOfficerPhoneNumberController}
import uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails.routes.{OrganisationTelephoneNumberController, TradingNameController}
import uk.gov.hmrc.disaaccountfrontend.controllers.orgemail.routes.EmailVerificationCodeController
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.{ChangeOfCircumstancesController, PeerToPeerPlatformController}
import uk.gov.hmrc.disaaccountfrontend.controllers.signatories.routes.{SignatoryCheckYourAnswersController, SignatoryJobTitleController}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CheckMode, Mode, NormalMode}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.pages.*
import uk.gov.hmrc.disaaccountfrontend.models.pages.signatories.RemoveSignatoryPage

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  def nextPage(page: Page, answers: Answers = Answers(), mode: Mode = NormalMode): Call = page match {
    case EnterYourOrganisationAddressPage   => OrganisationTelephoneNumberController.onPageLoad()
    // TODO: replace with the next page in the journey once it exists.
    case OrganisationTelephoneNumberPage    => OrganisationTelephoneNumberController.onPageLoad()
    case TradingNamePage                    => TradingNameController.onPageLoad()
    case InnovativeFinancialProductsPage    => innovativeFinancialProductsNextPage(answers)
    case PeerToPeerPlatformPage             => peerToPeerPlatformNextPage(answers)
    case FcaArticlesPage                    => fcaArticlesNextPage()
    case OrganisationEmailAddressPage       => EmailVerificationCodeController.onPageLoad()
    case EmailVerificationCodePage          => ChangeOfCircumstancesController.onPageLoad()
    // TODO: replace with the organisation email check-your-answers page once it exists.
    case FinancialOrganisationPage          => ChangeOfCircumstancesController.onPageLoad()
    case SignatoryNamePage(id, mode)        => signatoryNameNextPage(id, mode)
    case SignatoryJobTitlePage(id, mode)    => SignatoryCheckYourAnswersController.onPageLoad(id)
    // TODO: replace the TODOs in the RemoveSignatoryNextPage
    case RemoveSignatoryPage(_)             => RemoveSignatoryNextPage(answers)
    case LiaisonOfficerNamePage(id)         => liaisonOfficerNameNextPage(id, mode)
    case LiaisonOfficerEmailPage(id)        => liaisonOfficerEmailNextPage(id, mode)
    case LiaisonOfficerPhoneNumberPage(id)  => liaisonOfficerPhoneNumberNextPage(id, mode)
    case LiaisonOfficerCommunicationPage(_) => liaisonOfficerCommunicationNextPage(mode)
    case unsupportedPage                    =>
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
    // TODO: Replace this fallback with the FCA/FRN question (NOT FCA Articles!) when that page is implemented.
    ChangeOfCircumstancesController.onPageLoad()

  private def fcaArticlesNextPage(): Call =
    ChangeOfCircumstancesController.onPageLoad()

  private def signatoryNameNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => SignatoryJobTitleController.onPageLoad(id, NormalMode)
      case CheckMode  => SignatoryCheckYourAnswersController.onPageLoad(id)
    }

  private def RemoveSignatoryNextPage(answers: Answers): Call =
    answers.signatories match {
      // TODO Change to "You currently have a signatory" once ready
      case Some(_) => ChangeOfCircumstancesController.onPageLoad()
      // TODO Change to "Add a signatory" once ready
      case None    => ChangeOfCircumstancesController.onPageLoad()
    }

  private def liaisonOfficerNameNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerEmailController.onPageLoad(id, NormalMode)
      // TODO: Replace this fallback with the liaison officer check-your-answers page once it is implemented.
      case CheckMode  => ChangeOfCircumstancesController.onPageLoad()
    }

  private def liaisonOfficerEmailNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerPhoneNumberController.onPageLoad(id, NormalMode)
      // TODO: Replace this fallback with the liaison officer check-your-answers page once it is implemented.
      case CheckMode  => ChangeOfCircumstancesController.onPageLoad()
    }

  private def liaisonOfficerPhoneNumberNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerCommunicationController.onPageLoad(id, NormalMode)
      // TODO: Replace this fallback with the liaison officer check-your-answers page once it is implemented.
      case CheckMode  => ChangeOfCircumstancesController.onPageLoad()
    }

  private def liaisonOfficerCommunicationNextPage(mode: Mode): Call =
    mode match {
      // TODO: Replace these fallbacks with the liaison officer check-your-answers page once it is implemented.
      case NormalMode => ChangeOfCircumstancesController.onPageLoad()
      case CheckMode  => ChangeOfCircumstancesController.onPageLoad()
    }
}
