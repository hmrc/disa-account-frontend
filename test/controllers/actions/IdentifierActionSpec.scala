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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.AuthenticatedIdentifierAction
import uk.gov.hmrc.disaaccountfrontend.models.requests.IdentifierRequest
import uk.gov.hmrc.http.SessionKeys
import utils.BaseUnitSpec

import scala.concurrent.Future

class IdentifierActionSpec extends BaseUnitSpec {

  val defaultBodyParser: play.api.mvc.BodyParsers.Default = app.injector.instanceOf[play.api.mvc.BodyParsers.Default]

  def action(authConnector: uk.gov.hmrc.auth.core.AuthConnector): AuthenticatedIdentifierAction =
    new AuthenticatedIdentifierAction(authConnector, mockAppConfig, defaultBodyParser)

  def sessionRequest: FakeRequest[play.api.mvc.AnyContentAsEmpty.type] =
    FakeRequest().withSession(SessionKeys.sessionId -> testSessionId)

  "AuthenticatedIdentifierAction" should {

    "call the block with the zref, credential id and session id when authorised" in {
      val result = action(successfulAuthConnector()).invokeBlock(
        sessionRequest,
        (request: IdentifierRequest[_]) =>
          Future.successful(Ok(s"${request.zReference}-${request.credentialId}-${request.sessionId}"))
      )

      status(result)          shouldBe OK
      contentAsString(result) shouldBe s"$testZref-$testCredentialId-$testSessionId"
    }

    "redirect to unauthorised when the enrolment has no ZREF identifier" in {
      val enrolmentWithoutZref =
        Enrolments(Set(Enrolment("HMRC-DISA-ORG", Seq.empty[EnrolmentIdentifier], "Activated")))

      val result = action(successfulAuthConnector(enrolments = enrolmentWithoutZref)).invokeBlock(
        sessionRequest,
        (_: IdentifierRequest[_]) => Future.successful(Ok("should not reach here"))
      )

      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should contain(
        uk.gov.hmrc.disaaccountfrontend.controllers.routes.UnauthorisedController.onPageLoad().url
      )
    }

    "redirect to unauthorised when credentials are missing" in {
      val result = action(successfulAuthConnector(credentials = None)).invokeBlock(
        sessionRequest,
        (_: IdentifierRequest[_]) => Future.successful(Ok("should not reach here"))
      )

      status(result)         shouldBe SEE_OTHER
      redirectLocation(result) should contain(
        uk.gov.hmrc.disaaccountfrontend.controllers.routes.UnauthorisedController.onPageLoad().url
      )
    }

    "redirect to sign-in when there is no session id" in {
      val result = action(successfulAuthConnector()).invokeBlock(
        FakeRequest(),
        (_: IdentifierRequest[_]) => Future.successful(Ok("should not reach here"))
      )

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(authLoginStubSignInEndpoint)
    }

    "redirect to sign-in when there is no active session" in {
      val result = action(failingAuthConnector(uk.gov.hmrc.auth.core.MissingBearerToken())).invokeBlock(
        sessionRequest,
        (_: IdentifierRequest[_]) => Future.successful(Ok("should not reach here"))
      )

      status(result)             shouldBe SEE_OTHER
      redirectLocation(result).get should startWith(authLoginStubSignInEndpoint)
    }
  }
}
