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
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.CorrespondenceAddress
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class EnterYourOrganisationAddressControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-controller-test"
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

  val addressPath: String     = "/obligations/account/isa/enter-your-organisation-address"
  val registrationUrl: String = s"/disa-account/registration/$testZref"

  val registrationResponseBody: String =
    s"""{
       |  "groupId": "test-group-id",
       |  "organisationDetails": {
       |    "correspondenceAddress": ${Json.toJson(testCorrespondenceAddress)}
       |  }
       |}""".stripMargin

  def authenticatedGet(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, addressPath)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  def authenticatedPost(body: Map[String, Seq[String]]): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, addressPath)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)
      .withHeaders("Csrf-Token" -> "nocheck")
      .withFormUrlEncodedBody(body.view.mapValues(_.head).toSeq: _*)

  "GET /enter-your-organisation-address" should {

    "return 200 OK prefilled from disa-account when there are no cached answers" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationResponseBody)

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("1 Test Street")
    }

    "return 200 OK prefilled from the cache once answers have been saved" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationResponseBody)

      status(
        route(
          app,
          authenticatedPost(
            Map(
              "addressLine1" -> Seq("2 Cached Street"),
              "townOrCity"   -> Seq("Cached Town"),
              "postcode"     -> Seq("BB2 2BB")
            )
          )
        ).get
      ) shouldBe SEE_OTHER

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("2 Cached Street")
    }

    "redirect to the auth login stub for an unauthenticated request" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, addressPath)).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("auth-login-stub")
    }

    "return 500 InternalServerError and not render the form when disa-account fails, so the journey is not entered" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, INTERNAL_SERVER_ERROR, """{"statusCode":500,"message":"Boom"}""")

      val result = route(app, authenticatedGet()).get

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

  "POST /enter-your-organisation-address" should {

    "save the answer and redirect when the form is valid" in {
      stubAuth(testZref, testCredentialId)

      val result = route(
        app,
        authenticatedPost(
          Map(
            "addressLine1" -> Seq("1 Test Street"),
            "townOrCity"   -> Seq("Test Town"),
            "postcode"     -> Seq("AA1 1AA")
          )
        )
      ).get

      status(result) shouldBe SEE_OTHER

      val stored = await(repo.get(testSessionId))
      stored.map(_.updates.correspondenceAddress) shouldBe Some(
        Assign(
          CorrespondenceAddress(
            addressLine1 = Some("1 Test Street"),
            addressLine2 = None,
            addressLine3 = Some("Test Town"),
            postCode = Some("AA1 1AA")
          )
        )
      )
    }

    "return 400 BadRequest with the inline error when address line 1 is missing" in {
      stubAuth(testZref, testCredentialId)

      val result = route(
        app,
        authenticatedPost(
          Map(
            "addressLine1" -> Seq(""),
            "townOrCity"   -> Seq("Test Town"),
            "postcode"     -> Seq("AA1 1AA")
          )
        )
      ).get

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Enter address line 1, typically the building and street")
    }

    "return 400 BadRequest with the inline error when the postcode is an invalid format" in {
      stubAuth(testZref, testCredentialId)

      val result = route(
        app,
        authenticatedPost(
          Map(
            "addressLine1" -> Seq("1 Test Street"),
            "townOrCity"   -> Seq("Test Town"),
            "postcode"     -> Seq("ZZZZZ")
          )
        )
      ).get

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("Enter a postcode, like AA1 1AA")
    }
  }
}
