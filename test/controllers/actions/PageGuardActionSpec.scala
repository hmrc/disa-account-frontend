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

package controllers.actions

import play.api.mvc.Results.Ok
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.PageGuardActionImpl
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import uk.gov.hmrc.disaaccountfrontend.models.pages.InnovativeFinancialProductsPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import utils.BaseUnitSpec

import scala.concurrent.Future

class PageGuardActionSpec extends BaseUnitSpec {

  private val action = new PageGuardActionImpl()

  "PageGuardAction" should {

    "continue with the request when the page can be accessed" in {
      val result = action(InnovativeFinancialProductsPage).invokeBlock(
        dataRequest(Answers(isaProducts = Some(Seq(InnovativeFinanceIsas)))),
        _ => Future.successful(Ok)
      )

      status(result) shouldBe OK
    }

    "redirect to change of circumstances by default when the page cannot be accessed" in {
      val originalRoutePrefix = _root_.app.RoutesPrefix.prefix

      try {
        _root_.app.RoutesPrefix.setPrefix("/")
        val guardedAction = action(InnovativeFinancialProductsPage)
        _root_.app.RoutesPrefix.setPrefix(accountFrontendRoutePrefix)

        val result = guardedAction.invokeBlock(
          dataRequest(Answers(isaProducts = Some(Seq(CashIsas)))),
          _ => Future.successful(Ok)
        )

        status(result)         shouldBe SEE_OTHER
        redirectLocation(result) should contain(changeOfCircumstancesEndpoint)
      } finally _root_.app.RoutesPrefix.setPrefix(originalRoutePrefix)
    }

    "use a caller-supplied redirect when the page cannot be accessed" in {
      val redirect = Call("GET", customRedirectEndpoint)
      val result   = action(InnovativeFinancialProductsPage, redirect).invokeBlock(
        dataRequest(Answers()),
        _ => Future.successful(Ok)
      )

      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should contain(redirect.url)
    }
  }

  private def dataRequest(answers: Answers): DataRequest[play.api.mvc.AnyContentAsEmpty.type] =
    DataRequest(
      FakeRequest(),
      testZref,
      testCredentialId,
      testSessionId,
      sessionAnswers = None,
      effectiveAnswers = answers
    )
}
