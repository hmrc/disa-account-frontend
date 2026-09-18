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

package uk.gov.hmrc.disaaccountfrontend.controllers

import org.mongodb.scala.SingleObservableFuture
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, StocksAndSharesIsas}
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class ChangesCompletedControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-changes-completed-controller-test"
  private lazy val mongoUri: String                   = s"mongodb://127.0.0.1:27017/$databaseName"
  private lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(config)
      .overrides(play.api.inject.bind[MongoComponent].toInstance(mockMongoComponent))
      .build()

  private val repo: UserAnswersRepository = app.injector.instanceOf[UserAnswersRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repo.collection.drop().toFuture())
  }

  private val endpoint: String        = "/obligations/account/isa/changes-completed"
  private val registrationUrl: String = s"/disa-account/registration/$testZref"

  private def authenticatedGet(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, endpoint)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  private def stubRegistration(products: Seq[IsaProduct]): Unit =
    stubGet(
      registrationUrl,
      OK,
      Json
        .obj(
          "groupId"     -> "test-group-id",
          "isaProducts" -> Json.obj("isaProducts" -> products.map(_.toString))
        )
        .toString()
    )

  "GET /changes-completed" should {

    "show the updated ISA products section when session products differ from registration" in {
      stubAuth(testZref, testCredentialId)
      stubRegistration(Seq(CashIsas))
      await(
        repo.set(
          UserAnswers(testSessionId, SessionUpdates(isaProducts = Assign(Seq(CashIsas, StocksAndSharesIsas))))
        )
      )

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("Changes completed")
      contentAsString(result) should include("Updated ISA products")
    }

    "hide the updated ISA products section when products are unchanged" in {
      stubAuth(testZref, testCredentialId)
      stubRegistration(Seq(CashIsas))

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should not include "Updated ISA products"
    }

    "redirect an unauthenticated request to sign in" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, endpoint)).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("auth-login-stub")
    }
  }
}
