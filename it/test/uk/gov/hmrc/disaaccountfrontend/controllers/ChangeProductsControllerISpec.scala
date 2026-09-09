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

import org.jsoup.Jsoup
import org.mongodb.scala.SingleObservableFuture
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.CrowdFundedDebentures
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas, StocksAndSharesIsas}
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class ChangeProductsControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-change-products-controller-test"
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

  private val endpoint: String                   = "/obligations/account/isa/change-products"
  private val innovativeProductsEndpoint: String = "/obligations/account/isa/innovative-financial-products"
  private val registrationUrl: String            = s"/disa-account/registration/$testZref"

  private def registrationResponse(products: Seq[IsaProduct], includeDependents: Boolean = false): String = {
    val dependents =
      if (includeDependents) {
        Json.obj(
          "innovativeFinancialProducts" -> Json.arr(CrowdFundedDebentures.toString),
          "p2pPlatform"                  -> testP2pPlatform,
          "p2pPlatformNumber"            -> testP2pPlatformNumber
        )
      } else {
        Json.obj()
      }

    Json
      .obj(
        "groupId"     -> "test-group-id",
        "isaProducts" -> (Json.obj("isaProducts" -> products.map(_.toString)) ++ dependents),
        "signatories" -> Json.obj(
          "signatories" -> Json.arr(
            Json.obj(
              "id"       -> testSignatoryId,
              "fullName" -> testSignatoryName,
              "jobTitle" -> testSignatoryJobTitle,
              "email"    -> testSignatoryEmail
            )
          )
        )
      )
      .toString()
  }

  private def authenticatedGet(path: String = endpoint): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, path)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  private def authenticatedPost(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    authenticatedPostTo(endpoint, body: _*)

  private def authenticatedPostTo(
    path: String,
    body: (String, String)*
  ): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, path)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)
      .withHeaders("Csrf-Token" -> "nocheck")
      .withFormUrlEncodedBody(body: _*)

  private def checkboxIsChecked(html: String, value: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$value]").hasAttr("checked")

  "GET /change-products" should {

    "prefill ETMP products for an authenticated signatory" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail.toUpperCase))
      stubGet(
        registrationUrl,
        OK,
        registrationResponse(Seq(CashIsas, InnovativeFinanceIsas), includeDependents = true)
      )

      val result = route(app, authenticatedGet()).get
      val html   = contentAsString(result)

      status(result)                                      shouldBe OK
      checkboxIsChecked(html, CashIsas.toString)          shouldBe true
      checkboxIsChecked(html, InnovativeFinanceIsas.toString) shouldBe true
      checkboxIsChecked(html, StocksAndSharesIsas.toString)   shouldBe false
    }

    "redirect a non-signatory" in {
      stubAuth(testZref, testCredentialId, Some("someone.else@example.com"))
      stubGet(registrationUrl, OK, registrationResponse(Seq(CashIsas)))

      val result = route(app, authenticatedGet()).get

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).get should endWith("/change-of-circumstances")
    }

    "redirect an unauthenticated request to sign in" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, endpoint)).get

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).get should include("auth-login-stub")
    }
  }

  "POST /change-products" should {

    "clear Innovative Finance dependents and replay the retained products" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubGet(
        registrationUrl,
        OK,
        registrationResponse(Seq(CashIsas, InnovativeFinanceIsas), includeDependents = true)
      )

      val postResult = route(
        app,
        authenticatedPost(
          "value[2]" -> StocksAndSharesIsas.toString,
          "value[0]" -> CashIsas.toString
        )
      ).get

      status(postResult)                 shouldBe SEE_OTHER
      redirectLocation(postResult).get should endWith("/change-of-circumstances")
      await(repo.get(testSessionId)).map(_.updates) shouldBe Some(
        SessionUpdates(
          isaProducts = Assign(Seq(CashIsas, StocksAndSharesIsas)),
          innovativeFinancialProducts = Clear,
          p2pPlatform = Clear,
          p2pPlatformNumber = Clear
        )
      )

      val getResult = route(app, authenticatedGet()).get
      val html      = contentAsString(getResult)

      status(getResult)                                      shouldBe OK
      checkboxIsChecked(html, CashIsas.toString)              shouldBe true
      checkboxIsChecked(html, StocksAndSharesIsas.toString)   shouldBe true
      checkboxIsChecked(html, InnovativeFinanceIsas.toString) shouldBe false
    }

    "keep returning to Innovative Finance until its question is answered" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubGet(registrationUrl, OK, registrationResponse(Seq(CashIsas)))

      val selectedProducts = Seq(
        "value[0]" -> CashIsas.toString,
        "value[4]" -> InnovativeFinanceIsas.toString
      )

      val firstResult = route(app, authenticatedPost(selectedProducts: _*)).get

      status(firstResult)                 shouldBe SEE_OTHER
      redirectLocation(firstResult).get should endWith("/innovative-financial-products")
      await(repo.get(testSessionId)).map(_.updates.innovativeFinancialProducts) shouldBe Some(Clear)

      val browserBackResult = route(app, authenticatedPost(selectedProducts: _*)).get

      status(browserBackResult)                 shouldBe SEE_OTHER
      redirectLocation(browserBackResult).get should endWith("/innovative-financial-products")

      val innovativeResult = route(
        app,
        authenticatedPostTo(
          innovativeProductsEndpoint,
          "value[2]" -> CrowdFundedDebentures.toString
        )
      ).get

      status(innovativeResult) shouldBe SEE_OTHER

      val answeredResult = route(app, authenticatedPost(selectedProducts: _*)).get

      status(answeredResult)                 shouldBe SEE_OTHER
      redirectLocation(answeredResult).get should endWith("/change-of-circumstances")
    }

    "restore ETMP dependent answers when an original Innovative Finance product is reticked" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubGet(
        registrationUrl,
        OK,
        registrationResponse(Seq(CashIsas, InnovativeFinanceIsas), includeDependents = true)
      )
      await(
        repo.set(
          UserAnswers(
            testSessionId,
            SessionUpdates(
              isaProducts = Assign(Seq(CashIsas)),
              innovativeFinancialProducts = Clear,
              p2pPlatform = Clear,
              p2pPlatformNumber = Clear
            )
          )
        )
      )

      val result = route(
        app,
        authenticatedPost(
          "value[0]" -> CashIsas.toString,
          "value[4]" -> InnovativeFinanceIsas.toString
        )
      ).get

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).get should endWith("/change-of-circumstances")
      await(repo.get(testSessionId)).map(_.updates) shouldBe Some(
        SessionUpdates(
          isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)),
          innovativeFinancialProducts = Unchanged,
          p2pPlatform = Unchanged,
          p2pPlatformNumber = Unchanged
        )
      )

      val innovativeResult = route(app, authenticatedGet(innovativeProductsEndpoint)).get

      status(innovativeResult) shouldBe OK
      checkboxIsChecked(contentAsString(innovativeResult), CrowdFundedDebentures.toString) shouldBe true
    }

    "return Bad Request without saving when no product is selected" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubGet(registrationUrl, OK, registrationResponse(Seq(CashIsas)))

      val result = route(app, authenticatedPost()).get

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) should include("Select the ISA products your organisation offers")
      await(repo.get(testSessionId)) shouldBe None
    }

    "redirect a non-signatory without saving" in {
      stubAuth(testZref, testCredentialId, Some("someone.else@example.com"))
      stubGet(registrationUrl, OK, registrationResponse(Seq(CashIsas)))

      val result = route(app, authenticatedPost("value[0]" -> CashIsas.toString)).get

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).get should endWith("/change-of-circumstances")
      await(repo.get(testSessionId)) shouldBe None
    }
  }
}
