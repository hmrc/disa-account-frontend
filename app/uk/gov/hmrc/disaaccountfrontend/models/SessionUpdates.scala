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

package uk.gov.hmrc.disaaccountfrontend.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Unchanged
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.{InnovativeFinancialProduct, IsaProduct}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficers
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatories

case class SessionUpdates(
  correspondenceAddress: AnswerUpdate[CorrespondenceAddress] = Unchanged,
  organisationTelephoneNumber: AnswerUpdate[String] = Unchanged,
  tradingName: AnswerUpdate[String] = Unchanged,
  isaProducts: AnswerUpdate[Seq[IsaProduct]] = Unchanged,
  innovativeFinancialProducts: AnswerUpdate[Seq[InnovativeFinancialProduct]] = Unchanged,
  p2pPlatform: AnswerUpdate[String] = Unchanged,
  p2pPlatformNumber: AnswerUpdate[String] = Unchanged,
  fcaArticles: AnswerUpdate[Seq[FcaArticles]] = Unchanged,
  organisationEmailAddress: AnswerUpdate[String] = Unchanged,
  organisationEmailVerified: AnswerUpdate[Boolean] = Unchanged,
  financialOrganisation: AnswerUpdate[Seq[FinancialOrganisation]] = Unchanged,
  liaisonOfficers: AnswerUpdate[LiaisonOfficers] = Unchanged,
  signatories: AnswerUpdate[Signatories] = Unchanged
) {
  def getUpdatedEffectiveAnswers(answers: Answers): Answers =
    Answers(
      correspondenceAddress = correspondenceAddress.getEffectiveAnswer(answers.correspondenceAddress),
      organisationTelephoneNumber = organisationTelephoneNumber.getEffectiveAnswer(answers.organisationTelephoneNumber),
      tradingName = tradingName.getEffectiveAnswer(answers.tradingName),
      isaProducts = isaProducts.getEffectiveAnswer(answers.isaProducts),
      innovativeFinancialProducts = innovativeFinancialProducts.getEffectiveAnswer(answers.innovativeFinancialProducts),
      p2pPlatform = p2pPlatform.getEffectiveAnswer(answers.p2pPlatform),
      p2pPlatformNumber = p2pPlatformNumber.getEffectiveAnswer(answers.p2pPlatformNumber),
      fcaArticles = fcaArticles.getEffectiveAnswer(answers.fcaArticles),
      organisationEmailAddress = organisationEmailAddress.getEffectiveAnswer(answers.organisationEmailAddress),
      organisationEmailVerified = organisationEmailVerified.getEffectiveAnswer(answers.organisationEmailVerified),
      financialOrganisation = financialOrganisation.getEffectiveAnswer(answers.financialOrganisation),
      liaisonOfficers = liaisonOfficers.getEffectiveAnswer(answers.liaisonOfficers),
      signatories = signatories.getEffectiveAnswer(answers.signatories)
    )
}

object SessionUpdates {
  implicit val format: OFormat[SessionUpdates] = Json.format[SessionUpdates]
}
