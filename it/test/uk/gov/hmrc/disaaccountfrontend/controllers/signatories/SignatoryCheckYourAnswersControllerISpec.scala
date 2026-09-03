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

package uk.gov.hmrc.disaaccountfrontend.controllers.signatories

import org.mongodb.scala.SingleObservableFuture
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class SignatoryCheckYourAnswersControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-check-signatory-test"
  private lazy val mongoUri: String                   = s"mongodb://127.0.0.1:27017/$databaseName"
  private lazy val mockMongoComponent: MongoComponent = MongoComponent(mongoUri)

  override lazy val app: Application =
    new GuiceApplicationBuilder()
      .configure(config)
      .overrides(play.api.inject.bind[MongoComponent].toInstance(mockMongoComponent))
      .build()

  private val repo = app.injector.instanceOf[UserAnswersRepository]

  override def beforeEach(): Unit = {
    super.beforeEach()
    await(repo.collection.drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(repo.collection.drop().toFuture())
  }

  private val signatoryId     = "294da0a8-7484-4675-bce2-fe9195dc1bca"
  private val registrationUrl = s"/disa-account/registration/$testZref"
  private val routeUrl        = s"/obligations/account/isa/check-signatory-details?id=$signatoryId"

  private val registrationResponseBody =
    s"""{
       |  "signatories": {
       |    "signatories": [
       |      {
       |        "id": "$signatoryId",
       |        "fullName": "Test Signatory",
       |        "jobTitle": "Director"
       |      }
       |    ]
       |  }
       |}""".stripMargin

  private def authenticatedGet: FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, routeUrl)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  "GET /check-signatory-details" should {

    "render the signatory returned by disa-account" in {
      stubAuth(testZref, testCredentialId)
      stubGet(registrationUrl, OK, registrationResponseBody)

      val result = route(app, authenticatedGet).get

      status(result)        shouldBe OK
      contentAsString(result) should include("Check signatory details")
      contentAsString(result) should include("Test Signatory")
      contentAsString(result) should include("Director")
    }
  }
}
