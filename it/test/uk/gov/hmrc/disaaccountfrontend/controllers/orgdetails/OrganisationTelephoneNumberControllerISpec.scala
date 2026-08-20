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

package uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails

import org.mongodb.scala.SingleObservableFuture
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsFormUrlEncoded}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class OrganisationTelephoneNumberControllerISpec extends BaseIntegrationSpec {

  private val updatedTelephoneNumber = "07777777777"

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

  val telephoneNumberPath: String = "/obligations/account/isa/organisation-telephone-number"
  val registrationUrl: String     = s"/disa-account/registration/$testZref"

  val registrationResponseBody: String =
    s"""{
       |  "groupId": "test-group-id",
       |  "organisationDetails": {
       |    "orgTelephoneNumber": "$testOrgTelephoneNumber"
       |  }
       |}""".stripMargin

  def authenticatedGet(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, telephoneNumberPath)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  def authenticatedPost(body: Map[String, Seq[String]]): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, telephoneNumberPath)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)
      .withHeaders("Csrf-Token" -> "nocheck")
      .withFormUrlEncodedBody(body.view.mapValues(_.head).toSeq: _*)

  "GET /organisation-telephone-number" should {

    "return 200 OK prefilled from disa-account when there are no cached answers" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationResponseBody)

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include(testOrgTelephoneNumber)
    }

    "return 200 OK prefilled from the cache once an answer has been saved, in preference to disa-account" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationResponseBody)

      status(
        route(app, authenticatedPost(Map("value" -> Seq(updatedTelephoneNumber)))).get
      ) shouldBe SEE_OTHER

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include(updatedTelephoneNumber)
      contentAsString(result) should not include testOrgTelephoneNumber
    }

    "redirect to the auth login stub for an unauthenticated request" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, telephoneNumberPath)).get

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

  "POST /organisation-telephone-number" should {

    "save the answer and redirect when the form is valid" in {
      stubAuth(testZref, testCredentialId)

      val result = route(app, authenticatedPost(Map("value" -> Seq("01642123456")))).get

      status(result) shouldBe SEE_OTHER

      val stored = await(repo.get(testSessionId))
      stored.map(_.updates.organisationTelephoneNumber) shouldBe Some(Assign(testOrgTelephoneNumber))
    }

    "return 400 BadRequest with the inline error when the value is missing" in {
      stubAuth(testZref, testCredentialId)

      val result = route(app, authenticatedPost(Map("value" -> Seq("")))).get

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include(
        "Enter the phone number of your organisation. Use a UK phone number, like 01642 123 456 or 07777 777 777"
      )
    }

    "return 400 BadRequest with the inline error when the value is an invalid format" in {
      stubAuth(testZref, testCredentialId)

      val result = route(app, authenticatedPost(Map("value" -> Seq("0164212345a")))).get

      status(result)        shouldBe BAD_REQUEST
      contentAsString(result) should include("The phone number must not include letters a to z")
    }
  }
}
