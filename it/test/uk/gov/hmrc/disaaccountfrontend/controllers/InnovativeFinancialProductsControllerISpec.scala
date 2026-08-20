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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.{CrowdFundedDebentures, LongTermAssetFunds, PeertopeerLoansUsingAPlatformWith36hPermissions}
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.{CashIsas, InnovativeFinanceIsas}
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class InnovativeFinancialProductsControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-innovative-products-controller-test"
  private lazy val mongoUri: String                   = s"mongodb://127.0.0.1:27017/$databaseName"
  private lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(config)
      .overrides(play.api.inject.bind[MongoComponent].toInstance(mockMongoComponent))
      .build()

  val repo: UserAnswersRepository = app.injector.instanceOf[UserAnswersRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repo.collection.drop().toFuture())
  }

  val endpoint: String        = "/obligations/account/isa/innovative-financial-products"
  val registrationUrl: String = s"/disa-account/registration/$testZref"

  val enrolledRegistrationResponse: String =
    """{
      |  "groupId": "test-group-id",
      |  "isaProducts": {
      |    "isaProducts": ["cashIsas", "innovativeFinanceIsas"],
      |    "innovativeFinancialProducts": [
      |      "peerToPeerLoansUsingAPlatformWith36HPermissions",
      |      "crowdfundedDebentures"
      |    ]
      |  }
      |}""".stripMargin

  val registrationWithoutInnovativeFinanceIsa: String =
    """{
      |  "groupId": "test-group-id",
      |  "isaProducts": {
      |    "isaProducts": ["cashIsas"]
      |  }
      |}""".stripMargin

  def authenticatedGet(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, endpoint)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  def authenticatedPost(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, endpoint)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)
      .withHeaders("Csrf-Token" -> "nocheck")
      .withFormUrlEncodedBody(body: _*)

  private def checkboxIsChecked(html: String, value: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$value]").hasAttr("checked")

  "GET /innovative-financial-products" should {

    "prefill existing enrolment answers" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, enrolledRegistrationResponse)

      val result = route(app, authenticatedGet()).get
      val html   = contentAsString(result)

      status(result)                                                                    shouldBe OK
      checkboxIsChecked(html, PeertopeerLoansUsingAPlatformWith36hPermissions.toString) shouldBe true
      checkboxIsChecked(html, CrowdFundedDebentures.toString)                           shouldBe true
      checkboxIsChecked(html, LongTermAssetFunds.toString)                              shouldBe false
    }

    "allow access when Innovative Finance ISAs were added in the session" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationWithoutInnovativeFinanceIsa)
      await(
        repo.set(
          UserAnswers(
            testSessionId,
            SessionUpdates(isaProducts = Assign(Seq(CashIsas, InnovativeFinanceIsas)))
          )
        )
      )

      val result = route(app, authenticatedGet()).get

      status(result) shouldBe OK
    }

    "redirect when the session explicitly clears the enrolled Innovative Finance ISA selection" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, enrolledRegistrationResponse)
      await(repo.set(UserAnswers(testSessionId, SessionUpdates(isaProducts = Assign(Seq.empty)))))

      val result = route(app, authenticatedGet()).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should endWith("/change-of-circumstances")
    }

    "redirect an unauthenticated request to sign in" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, endpoint)).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("auth-login-stub")
    }
  }

  "POST /innovative-financial-products" should {

    "store the selected answers and replay them on a later GET" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, enrolledRegistrationResponse)

      val postResult = route(
        app,
        authenticatedPost(
          "value[3]" -> LongTermAssetFunds.toString,
          "value[2]" -> CrowdFundedDebentures.toString
        )
      ).get

      status(postResult)                                                        shouldBe SEE_OTHER
      redirectLocation(postResult).get                                            should endWith("/change-of-circumstances")
      await(repo.get(testSessionId)).map(_.updates.innovativeFinancialProducts) shouldBe Some(
        Assign(Seq(CrowdFundedDebentures, LongTermAssetFunds))
      )

      val getResult = route(app, authenticatedGet()).get
      val html      = contentAsString(getResult)

      status(getResult)                                                                 shouldBe OK
      checkboxIsChecked(html, CrowdFundedDebentures.toString)                           shouldBe true
      checkboxIsChecked(html, LongTermAssetFunds.toString)                              shouldBe true
      checkboxIsChecked(html, PeertopeerLoansUsingAPlatformWith36hPermissions.toString) shouldBe false
    }

    "return Bad Request with the required error when every option is deselected" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, enrolledRegistrationResponse)

      val result = route(app, authenticatedPost()).get

      status(result)                 shouldBe BAD_REQUEST
      contentAsString(result)          should include(
        "Select which types of innovative finance products your organisation will offer"
      )
      await(repo.get(testSessionId)) shouldBe None
    }
  }
}
