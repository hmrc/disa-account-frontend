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

import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails

case class EffectiveAnswers(
  isaProducts: Option[Seq[IsaProduct]],
  innovativeFinancialProducts: Option[Seq[InnovativeFinancialProduct]]
) {
  def hasInnovativeFinanceIsa: Boolean = isaProducts.exists(_.contains(InnovativeFinanceIsas))
}

object EffectiveAnswers {

  def from(
    registrationDetails: Option[RegistrationDetails],
    sessionUpdates: Option[SessionUpdates]
  ): EffectiveAnswers =
    EffectiveAnswers(
      isaProducts = sessionUpdates.flatMap(_.isaProducts).orElse(registrationDetails.flatMap(_.isaProductSelections)),
      innovativeFinancialProducts = sessionUpdates
        .flatMap(_.innovativeFinancialProducts)
        .orElse(registrationDetails.flatMap(_.innovativeFinancialProductSelections))
    )
}
