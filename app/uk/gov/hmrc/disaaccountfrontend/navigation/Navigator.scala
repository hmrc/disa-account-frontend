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
import uk.gov.hmrc.disaaccountfrontend.controllers.liaisonofficers.routes.{AddedLiaisonOfficersController, LiaisonOfficerCheckYourAnswersController, LiaisonOfficerCommunicationController, LiaisonOfficerEmailController, LiaisonOfficerNameController, LiaisonOfficerPhoneNumberController}
import uk.gov.hmrc.disaaccountfrontend.controllers.orgemail.routes.EmailVerificationCodeController
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.{ChangeOfCircumstancesController, InnovativeFinancialProductsController, PeerToPeerPlatformController, PeerToPeerPlatformNumberController}
import uk.gov.hmrc.disaaccountfrontend.controllers.signatories.routes.{AddedSignatoryController, SignatoryCheckYourAnswersController, SignatoryJobTitleController, SignatoryNameController}
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.{No, Yes}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CheckMode, Mode, NormalMode, YesNoAnswer}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.pages.*
import uk.gov.hmrc.disaaccountfrontend.models.pages.liaisonofficers.{LiaisonOfficerCheckYourAnswersPage, LiaisonOfficerCommunicationPage, LiaisonOfficerEmailPage, LiaisonOfficerNamePage, LiaisonOfficerPhoneNumberPage, RemoveLiaisonOfficerPage}
import uk.gov.hmrc.disaaccountfrontend.models.pages.signatories.{RemoveSignatoryPage, SignatoryJobTitlePage, SignatoryNamePage}

import javax.inject.{Inject, Singleton}

@Singleton
class Navigator @Inject() () {

  def nextPageFromAddedSignatories(answer: YesNoAnswer): Call = answer match {
    case Yes => SignatoryNameController.onPageLoad(None, NormalMode)
    case No  => ChangeOfCircumstancesController.onPageLoad()
  }

  def nextPageFromAddedLiaisonOfficer(answer: YesNoAnswer): Call = answer match {
    case Yes => LiaisonOfficerNameController.onPageLoad(None, NormalMode)
    case No  => ChangeOfCircumstancesController.onPageLoad()
  }

  def nextPageFromChangeProducts(answers: Answers): Call = {
    val innovativeFinanceIsSelected     = answers.isaProducts.exists(_.contains(InnovativeFinanceIsas))
    val innovativeFinanceIsAnswered     = answers.innovativeFinancialProducts.exists(_.nonEmpty)
    val innovativeFinanceAnswerRequired = innovativeFinanceIsSelected && !innovativeFinanceIsAnswered

    if (innovativeFinanceAnswerRequired) {
      InnovativeFinancialProductsController.onPageLoad()
    } else {
      ChangeOfCircumstancesController.onPageLoad()
    }
  }

  def nextPage(page: Page, answers: Answers = Answers(), mode: Mode = NormalMode): Call = page match {
    case EnterYourOrganisationAddressPage      => ChangeOfCircumstancesController.onPageLoad()
    case OrganisationTelephoneNumberPage       => ChangeOfCircumstancesController.onPageLoad()
    case TradingNamePage                       => ChangeOfCircumstancesController.onPageLoad()
    case InnovativeFinancialProductsPage       => innovativeFinancialProductsNextPage(answers)
    case PeerToPeerPlatformPage                => peerToPeerPlatformNextPage(answers)
    case PeerToPeerPlatformNumberPage          => peerToPeerPlatformNumberNextPage()
    case FcaArticlesPage                       => fcaArticlesNextPage()
    case OrganisationEmailAddressPage          => EmailVerificationCodeController.onPageLoad()
    case EmailVerificationCodePage             => ChangeOfCircumstancesController.onPageLoad()
    // TODO: replace with the organisation email check-your-answers page once it exists.
    case FinancialOrganisationPage             => ChangeOfCircumstancesController.onPageLoad()
    case SignatoryNamePage(id, mode)           => signatoryNameNextPage(id, mode)
    case SignatoryJobTitlePage(id, mode)       => SignatoryCheckYourAnswersController.onPageLoad(id)
    case RemoveSignatoryPage(_)                => removeSignatoryNextPage(answers)
    case LiaisonOfficerNamePage(id)            => liaisonOfficerNameNextPage(id, mode)
    case LiaisonOfficerEmailPage(id)           => liaisonOfficerEmailNextPage(id, mode)
    case LiaisonOfficerPhoneNumberPage(id)     => liaisonOfficerPhoneNumberNextPage(id, mode)
    case LiaisonOfficerCommunicationPage(id)   => LiaisonOfficerCheckYourAnswersController.onPageLoad(id)
    case LiaisonOfficerCheckYourAnswersPage(_) => AddedLiaisonOfficersController.onPageLoad()
    case RemoveLiaisonOfficerPage(_)           => removeLiaisonOfficerPageNextPage(answers)
    case unsupportedPage                       =>
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
    PeerToPeerPlatformNumberController.onPageLoad()

  private def peerToPeerPlatformNumberNextPage(): Call =
    ChangeOfCircumstancesController.onPageLoad()

  private def fcaArticlesNextPage(): Call =
    ChangeOfCircumstancesController.onPageLoad()

  private def signatoryNameNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => SignatoryJobTitleController.onPageLoad(id, NormalMode)
      case CheckMode  => SignatoryCheckYourAnswersController.onPageLoad(id)
    }

  private def removeSignatoryNextPage(answers: Answers): Call =
    if (answers.signatories.exists(_.signatories.exists(_.isComplete))) {
      AddedSignatoryController.onPageLoad()
    } else {
      SignatoryNameController.onPageLoad(None, NormalMode)
    }

  private def liaisonOfficerNameNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerEmailController.onPageLoad(id, NormalMode)
      case CheckMode  => LiaisonOfficerCheckYourAnswersController.onPageLoad(id)
    }

  private def liaisonOfficerEmailNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerPhoneNumberController.onPageLoad(id, NormalMode)
      case CheckMode  => LiaisonOfficerCheckYourAnswersController.onPageLoad(id)
    }

  private def liaisonOfficerPhoneNumberNextPage(id: String, mode: Mode): Call =
    mode match {
      case NormalMode => LiaisonOfficerCommunicationController.onPageLoad(id, NormalMode)
      case CheckMode  => LiaisonOfficerCheckYourAnswersController.onPageLoad(id)
    }

  private def removeLiaisonOfficerPageNextPage(answers: Answers): Call =
    if (answers.liaisonOfficers.exists(_.liaisonOfficers.exists(_.isComplete))) {
      AddedLiaisonOfficersController.onPageLoad()
    } else {
      LiaisonOfficerNameController.onPageLoad(None, NormalMode)
    }
}
