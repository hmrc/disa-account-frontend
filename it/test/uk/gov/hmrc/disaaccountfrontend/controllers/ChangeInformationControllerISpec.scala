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
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.*
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatories
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.mongo.MongoComponent

class ChangeInformationControllerISpec extends BaseIntegrationSpec {

  private val databaseName: String                    = "disa-account-frontend-change-information-controller-test"
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

  val endpoint: String        = "/obligations/account/isa/change-information"
  val registrationUrl: String = s"/disa-account/registration/$testZref"

  def authenticatedGet(): FakeRequest[AnyContentAsEmpty.type] =
    FakeRequest(GET, endpoint)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)

  def authenticatedPost(body: (String, String)*): FakeRequest[AnyContentAsFormUrlEncoded] =
    FakeRequest(POST, endpoint)
      .withSession(SessionKeys.authToken -> "Bearer mock-bearer-token", SessionKeys.sessionId -> testSessionId)
      .withHeaders("Csrf-Token" -> "nocheck")
      .withFormUrlEncodedBody(body: _*)

  private def stubRegistrationNotFound(): Unit =
    stubGet(registrationUrl, NOT_FOUND, """{"statusCode":404,"message":"Not found"}""")

  private def stubRegistrationWithSignatory(): Unit =
    stubGet(
      registrationUrl,
      OK,
      Json
        .obj(
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
    )

  private def checkboxIsChecked(html: String, value: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$value]").hasAttr("checked")

  "GET /change-information" should {

    "render the page for an authenticated user" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("What would you like to change?")
    }

    "show ISA product information when the authenticated email matches a signatory" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail.toUpperCase))
      stubRegistrationWithSignatory()

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("ISA Product information")
    }

    "continue showing ISA product information after the signatory removes themselves from the session answers" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubRegistrationWithSignatory()
      await(
        repo.set(
          UserAnswers(
            testSessionId,
            SessionUpdates(signatories = Assign(Signatories()))
          )
        )
      )

      val result = route(app, authenticatedGet()).get

      status(result)        shouldBe OK
      contentAsString(result) should include("ISA Product information")
    }

    "replay selections saved in Mongo" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()
      await(
        repo.set(
          UserAnswers(
            testSessionId,
            SessionUpdates(changeInformationSelections = Assign(Seq(OrganisationInformation, AuthorisedUsers)))
          )
        )
      )

      val result = route(app, authenticatedGet()).get
      val html   = contentAsString(result)

      status(result)                                            shouldBe OK
      checkboxIsChecked(html, OrganisationInformation.toString) shouldBe true
      checkboxIsChecked(html, IsaProductInformation.toString)   shouldBe false
      checkboxIsChecked(html, AuthorisedUsers.toString)         shouldBe true
      checkboxIsChecked(html, viewAllInformationFormValue)      shouldBe false
    }

    "replay view all information saved in Mongo" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()
      await(
        repo.set(
          UserAnswers(
            testSessionId,
            SessionUpdates(changeInformationSelections = Assign(Seq(ViewAllInformation)))
          )
        )
      )

      val result = route(app, authenticatedGet()).get
      val html   = contentAsString(result)

      status(result)                                            shouldBe OK
      checkboxIsChecked(html, OrganisationInformation.toString) shouldBe false
      checkboxIsChecked(html, AuthorisedUsers.toString)         shouldBe false
      checkboxIsChecked(html, viewAllInformationFormValue)      shouldBe true
    }

    "redirect an unauthenticated request to sign in" in {
      stubAuthFail()

      val result = route(app, FakeRequest(GET, endpoint)).get

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should include("auth-login-stub")
    }
  }

  "POST /change-information" should {

    "store selections and replay them on a later GET" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()

      val postResult = route(
        app,
        authenticatedPost(
          "value[]" -> AuthorisedUsers.toString,
          "value[]" -> OrganisationInformation.toString
        )
      ).get

      status(postResult)                                                        shouldBe SEE_OTHER
      redirectLocation(postResult).get                                            should endWith("/change-of-circumstances")
      await(repo.get(testSessionId)).map(_.updates.changeInformationSelections) shouldBe Some(
        Assign(Seq(OrganisationInformation, AuthorisedUsers))
      )

      val getResult = route(app, authenticatedGet()).get
      val html      = contentAsString(getResult)

      status(getResult)                                         shouldBe OK
      checkboxIsChecked(html, OrganisationInformation.toString) shouldBe true
      checkboxIsChecked(html, IsaProductInformation.toString)   shouldBe false
      checkboxIsChecked(html, AuthorisedUsers.toString)         shouldBe true
    }

    "store and replay view all information for a non-signatory" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()

      val result = route(
        app,
        authenticatedPost(
          "value[]" -> OrganisationInformation.toString,
          "value[]" -> viewAllInformationFormValue
        )
      ).get

      status(result)                                                            shouldBe SEE_OTHER
      await(repo.get(testSessionId)).map(_.updates.changeInformationSelections) shouldBe Some(
        Assign(Seq(ViewAllInformation))
      )

      val getResult = route(app, authenticatedGet()).get

      status(getResult)                                                          shouldBe OK
      checkboxIsChecked(contentAsString(getResult), viewAllInformationFormValue) shouldBe true
    }

    "store ISA product information when submitted by a signatory" in {
      stubAuth(testZref, testCredentialId, Some(testSignatoryEmail))
      stubRegistrationWithSignatory()

      val result = route(app, authenticatedPost("value[]" -> IsaProductInformation.toString)).get

      status(result)                                                            shouldBe SEE_OTHER
      await(repo.get(testSessionId)).map(_.updates.changeInformationSelections) shouldBe Some(
        Assign(Seq(IsaProductInformation))
      )
    }

    "reject ISA product information when submitted by a non-signatory" in {
      stubAuth(testZref, testCredentialId, Some("someone.else@example.com"))
      stubRegistrationWithSignatory()

      val result = route(app, authenticatedPost("value[]" -> IsaProductInformation.toString)).get

      status(result)                 shouldBe BAD_REQUEST
      await(repo.get(testSessionId)) shouldBe None
    }

    "return Bad Request without storing answers when no option is selected" in {
      stubAuth(testZref, testCredentialId)
      stubRegistrationNotFound()

      val result = route(app, authenticatedPost()).get

      status(result)                 shouldBe BAD_REQUEST
      contentAsString(result)          should include("Select from the options what you would like to change")
      await(repo.get(testSessionId)) shouldBe None
    }

    "redirect an unauthenticated request to sign in without storing answers" in {
      stubAuthFail()

      val request = FakeRequest(POST, endpoint)
        .withHeaders("Csrf-Token" -> "nocheck")
        .withFormUrlEncodedBody("value[]" -> OrganisationInformation.toString)
      val result  = route(app, request).get

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).get     should include("auth-login-stub")
      await(repo.get(testSessionId)) shouldBe None
    }
  }
}
