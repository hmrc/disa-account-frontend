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

import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates}

case object InnovativeFinancialProductsPage extends GuardedPage with PageWithAnswers[Set[InnovativeFinancialProduct]] {

  def canBeAccessedWith(answers: Answers): Boolean =
    answers.isaProducts.exists(_.contains(InnovativeFinanceIsas))

  def saveAnswerAndHandleDependents(
    request: DataRequest[_],
    newAnswer: Set[InnovativeFinancialProduct]
  ): SessionUpdates = {
    val existingUpdates           = request.sessionAnswers.fold(SessionUpdates())(_.updates)
    val platformProductWasRemoved =
      request.effectiveAnswers.innovativeFinancialProducts.exists(
        _.contains(PeertopeerLoansUsingAPlatformWith36hPermissions)
      ) && !newAnswer.contains(PeertopeerLoansUsingAPlatformWith36hPermissions)
    val hasPlatformAnswers        =
      request.effectiveAnswers.p2pPlatform.isDefined || request.effectiveAnswers.p2pPlatformNumber.isDefined

    if (platformProductWasRemoved && hasPlatformAnswers) {
      existingUpdates.copy(
        innovativeFinancialProducts = Assign(newAnswer.toSeq),
        p2pPlatform = Clear,
        p2pPlatformNumber = Clear
      )
    } else {
      existingUpdates.copy(innovativeFinancialProducts = Assign(newAnswer.toSeq))
    }
  }
}
