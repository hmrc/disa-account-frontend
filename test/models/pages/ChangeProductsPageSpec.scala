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

package models.pages

import play.api.test.FakeRequest
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.CrowdFundedDebentures
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.*
import uk.gov.hmrc.disaaccountfrontend.models.pages.ChangeProductsPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class ChangeProductsPageSpec extends BaseUnitSpec {

  private def request(
    originalProducts: Seq[IsaProduct],
    effectiveProducts: Seq[IsaProduct],
    existingUpdates: SessionUpdates
  ): DataRequest[_] =
    DataRequest(
      FakeRequest(),
      testZref,
      testCredentialId,
      None,
      testSessionId,
      Some(UserAnswers(testSessionId, existingUpdates)),
      Answers(isaProducts = Some(originalProducts)),
      Answers(isaProducts = Some(effectiveProducts))
    )

  "ChangeProductsPage" should {

    "save products in display order and preserve unrelated updates" in {
      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      val dataRequest     = request(Seq(CashIsas), Seq(CashIsas), existingUpdates)

      ChangeProductsPage.saveAnswerAndHandleDependents(
        dataRequest,
        Set(StocksAndSharesIsas, CashIsas)
      ) shouldBe existingUpdates.copy(isaProducts = Assign(Seq(CashIsas, StocksAndSharesIsas)))
    }

    "clear all Innovative Finance answers when the product is deselected" in {
      val existingUpdates = SessionUpdates(
        innovativeFinancialProducts = Assign(Seq(CrowdFundedDebentures)),
        p2pPlatform = Assign(testP2pPlatform),
        p2pPlatformNumber = Assign(testP2pPlatformNumber)
      )
      val dataRequest     = request(
        Seq(CashIsas, InnovativeFinanceIsas),
        Seq(CashIsas, InnovativeFinanceIsas),
        existingUpdates
      )

      ChangeProductsPage.saveAnswerAndHandleDependents(dataRequest, Set(CashIsas)) shouldBe
        existingUpdates.copy(
          isaProducts = Assign(Seq(CashIsas)),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
    }

    "restore ETMP answers when an originally offered Innovative Finance ISA is reselected" in {
      val existingUpdates = SessionUpdates(
        isaProducts = Assign(Seq(CashIsas)),
        innovativeFinancialProducts = Clear,
        p2pPlatform = Clear,
        p2pPlatformNumber = Clear
      )
      val dataRequest     = request(
        Seq(CashIsas, InnovativeFinanceIsas),
        Seq(CashIsas),
        existingUpdates
      )

      ChangeProductsPage.saveAnswerAndHandleDependents(
        dataRequest,
        Set(CashIsas, InnovativeFinanceIsas)
      ) shouldBe existingUpdates.copy(
        isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Unchanged,
        p2pPlatform = Unchanged,
        p2pPlatformNumber = Unchanged
      )
    }

    "clear dependent answers when Innovative Finance is newly selected" in {
      val existingUpdates = SessionUpdates(
        innovativeFinancialProducts = Assign(Seq(CrowdFundedDebentures)),
        p2pPlatform = Assign(testP2pPlatform),
        p2pPlatformNumber = Assign(testP2pPlatformNumber)
      )
      val dataRequest     = request(Seq(CashIsas), Seq(CashIsas), existingUpdates)

      ChangeProductsPage.saveAnswerAndHandleDependents(
        dataRequest,
        Set(CashIsas, InnovativeFinanceIsas)
      ) shouldBe existingUpdates.copy(
        isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
        innovativeFinancialProducts = Clear,
        p2pPlatform = Clear,
        p2pPlatformNumber = Clear
      )
    }

    "preserve dependent updates while Innovative Finance remains selected" in {
      val existingUpdates = SessionUpdates(
        innovativeFinancialProducts = Assign(Seq(CrowdFundedDebentures)),
        p2pPlatform = Assign(testP2pPlatform)
      )
      val dataRequest     = request(
        Seq(CashIsas, InnovativeFinanceIsas),
        Seq(CashIsas, InnovativeFinanceIsas),
        existingUpdates
      )

      ChangeProductsPage.saveAnswerAndHandleDependents(
        dataRequest,
        Set(CashIsas, InnovativeFinanceIsas)
      ) shouldBe existingUpdates.copy(isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)))
    }
  }
}
