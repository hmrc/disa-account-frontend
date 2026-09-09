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

package uk.gov.hmrc.disaaccountfrontend.models.pages

import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.SessionUpdates

case object ChangeProductsPage extends PageWithAnswers[Set[IsaProduct]] {

  override def saveAnswerAndHandleDependents(
    request: DataRequest[_],
    newAnswer: Set[IsaProduct]
  ): SessionUpdates = {
    val existingUpdates           = request.sessionAnswers.fold(SessionUpdates())(_.updates)
    val orderedAnswer             = IsaProduct.values.filter(newAnswer.contains)
    val innovativeFinanceWasSelected =
      request.effectiveAnswers.isaProducts.exists(_.contains(InnovativeFinanceIsas))
    val innovativeFinanceIsSelected  = newAnswer.contains(InnovativeFinanceIsas)
    val innovativeFinanceWasOriginal =
      request.originalAnswers.isaProducts.exists(_.contains(InnovativeFinanceIsas))

    (innovativeFinanceWasSelected, innovativeFinanceIsSelected, innovativeFinanceWasOriginal) match {
      case (true, false, _) =>
        existingUpdates.copy(
          isaProducts = Assign(orderedAnswer),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
      case (false, true, true) =>
        existingUpdates.copy(
          isaProducts = Assign(orderedAnswer),
          innovativeFinancialProducts = Unchanged,
          p2pPlatform = Unchanged,
          p2pPlatformNumber = Unchanged
        )
      case (false, true, false) =>
        existingUpdates.copy(
          isaProducts = Assign(orderedAnswer),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
      case _ =>
        existingUpdates.copy(isaProducts = Assign(orderedAnswer))
    }
  }
}
