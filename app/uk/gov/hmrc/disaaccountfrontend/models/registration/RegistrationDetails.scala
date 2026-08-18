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
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.{InnovativeFinancialProduct, IsaProduct, IsaProducts}

case class OrganisationDetails(
  correspondenceAddress: Option[CorrespondenceAddress] = None,
  orgTelephoneNumber: Option[String] = None
)

object OrganisationDetails {
  implicit val reads: Reads[OrganisationDetails] = Json.reads[OrganisationDetails]
}

case class RegistrationDetails(
  organisationDetails: Option[OrganisationDetails] = None,
  isaProducts: Option[IsaProducts] = None
) {
  def correspondenceAddress: Option[CorrespondenceAddress] = organisationDetails.flatMap(_.correspondenceAddress)
  def orgTelephoneNumber: Option[String]                   = organisationDetails.flatMap(_.orgTelephoneNumber)

  def isaProductSelections: Option[Seq[IsaProduct]] = isaProducts.flatMap(_.isaProducts)

  def innovativeFinancialProductSelections: Option[Seq[InnovativeFinancialProduct]] =
    isaProducts.flatMap(_.innovativeFinancialProducts)
}

object RegistrationDetails {
  implicit val reads: Reads[RegistrationDetails] = Json.reads[RegistrationDetails]
}
