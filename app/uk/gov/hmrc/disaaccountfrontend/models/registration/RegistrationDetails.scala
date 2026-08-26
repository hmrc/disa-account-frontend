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

package uk.gov.hmrc.disaaccountfrontend.models.registration

import play.api.libs.json.{Json, Reads}
import uk.gov.hmrc.disaaccountfrontend.models.CorrespondenceAddress
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.{CertificatesOfAuthority, FinancialOrganisation}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}

case class OrganisationDetails(
  correspondenceAddress: Option[CorrespondenceAddress] = None,
  orgTelephoneNumber: Option[String] = None,
  tradingName: Option[String] = None
)

object OrganisationDetails {
  implicit val reads: Reads[OrganisationDetails] = Json.reads[OrganisationDetails]
}

case class OrganisationEmail(organisationEmail: Option[String] = None, verified: Option[Boolean] = None)

object OrganisationEmail {
  implicit val reads: Reads[OrganisationEmail] = Json.reads[OrganisationEmail]
}

case class RegistrationDetails(
  organisationDetails: Option[OrganisationDetails] = None,
  organisationEmail: Option[OrganisationEmail] = None,
  isaProducts: Option[IsaProducts] = None,
  certificatesOfAuthority: Option[CertificatesOfAuthority] = None,
  signatories: Option[Signatories] = None
) {
  def correspondenceAddress: Option[CorrespondenceAddress] = organisationDetails.flatMap(_.correspondenceAddress)
  def orgTelephoneNumber: Option[String]                   = organisationDetails.flatMap(_.orgTelephoneNumber)
  def tradingName: Option[String]                          = organisationDetails.flatMap(_.tradingName)
  def organisationEmailAddress: Option[String]             = organisationEmail.flatMap(_.organisationEmail)
  def signatoriesList: Option[Seq[Signatory]]              = signatories.map(_.signatories)

  def isaProductSelections: Option[Seq[IsaProduct]] = isaProducts.flatMap(_.isaProducts)

  def innovativeFinancialProductSelections: Option[Seq[InnovativeFinancialProduct]] =
    isaProducts.flatMap(_.innovativeFinancialProducts)

  def p2pPlatform: Option[String] = isaProducts.flatMap(_.p2pPlatform)

  def p2pPlatformNumber: Option[String] = isaProducts.flatMap(_.p2pPlatformNumber)

  def financialOrganisation: Option[Seq[FinancialOrganisation]] =
    certificatesOfAuthority.flatMap(_.financialOrganisation)
}

object RegistrationDetails {
  implicit val reads: Reads[RegistrationDetails] = Json.reads[RegistrationDetails]
}
