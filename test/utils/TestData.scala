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

package utils

import uk.gov.hmrc.auth.core.retrieve.Credentials
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.disaaccountfrontend.models.CorrespondenceAddress
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation.{BuildingSociety, InsuranceCompany}
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.{CertificatesOfAuthority, FinancialOrganisation}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, PeertopeerLoansAndHave36hPermissions}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import uk.gov.hmrc.disaaccountfrontend.models.registration.{OrganisationDetails, OrganisationEmail, RegistrationDetails}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}

trait TestData {
  val testZref: String         = "Z1234"
  val testCredentialId: String = "cred-1234"
  val testSessionId: String    = "session-1234"

  val testEnrolments: Enrolments   = Enrolments(
    Set(Enrolment("HMRC-DISA-ORG", Seq(EnrolmentIdentifier("ZREF", testZref)), "Activated"))
  )
  val testCredentials: Credentials = Credentials(testCredentialId, "GovernmentGateway")

  val testCorrespondenceAddress: CorrespondenceAddress = CorrespondenceAddress(
    addressLine1 = Some("1 Test Street"),
    addressLine2 = Some("Test Town"),
    addressLine3 = Some("Test County"),
    postCode = Some("AA1 1AA")
  )

  val testOrgTelephoneNumber: String       = "01642123456"
  val testOrganisationEmailAddress: String = "test@example.com"
  val testP2pPlatform: String              = "Platform r Us"
  val testP2pPlatformNumber: String        = "1234567"

  val testRegistrationDetails: RegistrationDetails = RegistrationDetails(
    organisationDetails = Some(
      OrganisationDetails(
        correspondenceAddress = Some(testCorrespondenceAddress),
        orgTelephoneNumber = Some(testOrgTelephoneNumber)
      )
    ),
    organisationEmail = Some(
      OrganisationEmail(organisationEmail = Some(testOrganisationEmailAddress))
    )
  )

  val testIsaProductSelections: Seq[IsaProduct] = Seq(CashIsas, InnovativeFinanceIsas)

  val testInnovativeFinancialProductSelections: Seq[InnovativeFinancialProduct] =
    Seq(PeertopeerLoansAndHave36hPermissions, CrowdFundedDebentures)

  val testRegistrationDetailsWithInnovativeFinanceIsa: RegistrationDetails =
    testRegistrationDetails.copy(
      isaProducts = Some(
        IsaProducts(
          isaProducts = Some(testIsaProductSelections),
          innovativeFinancialProducts = Some(testInnovativeFinancialProductSelections),
          p2pPlatform = Some(testP2pPlatform),
          p2pPlatformNumber = Some(testP2pPlatformNumber)
        )
      )
    )

  val testFinancialOrganisationSelections: Seq[FinancialOrganisation] =
    Seq(BuildingSociety, InsuranceCompany)

  val testRegistrationDetailsWithFinancialOrganisation: RegistrationDetails =
    testRegistrationDetailsWithInnovativeFinanceIsa.copy(
      certificatesOfAuthority = Some(
        CertificatesOfAuthority(financialOrganisation = Some(testFinancialOrganisationSelections))
      )
    )

  val testSignatoryId: String   = "signatory-1"
  val testSignatoryName: String = "Jane Smith"

  val testSignatories: Seq[Signatory] = Seq(Signatory(testSignatoryId, fullName = Some(testSignatoryName)))

  val testRegistrationDetailsWithSignatories: RegistrationDetails =
    testRegistrationDetailsWithFinancialOrganisation.copy(
      signatories = Some(Signatories(testSignatories))
    )
}
