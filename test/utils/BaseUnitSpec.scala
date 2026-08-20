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

package utils

import com.typesafe.config.Config
import controllers.actions.{FakeDataRetrievalAction, FakeIdentifierAction}
import org.apache.pekko.actor.ActorSystem
import org.mockito.Mockito
import org.scalatest.{BeforeAndAfterEach, EitherValues, OptionValues}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers.stubControllerComponents
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{AuthenticatedIdentifierAction, DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.ExecutionContext

abstract class BaseUnitSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with EitherValues
    with OptionValues
    with ScalaFutures
    with MockitoSugar
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite
    with TestData
    with TestEndpoints
    with AuthTestSupport {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  lazy val retryConfig: Config      = app.configuration.underlying
  lazy val actorSystem: ActorSystem = app.actorSystem

  val mockHttpClient: HttpClientV2                     = mock[HttpClientV2]
  val mockAppConfig: AppConfig                         = mock[AppConfig]
  val mockRequestBuilder: RequestBuilder               = mock[RequestBuilder]
  val mockAuthConnector: AuthConnector                 = mock[AuthConnector]
  val mockRegistrationConnector: RegistrationConnector = mock[RegistrationConnector]
  val mockUserAnswersRepository: UserAnswersRepository = mock[UserAnswersRepository]

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockHttpClient,
      mockAppConfig,
      mockRequestBuilder,
      mockAuthConnector,
      mockRegistrationConnector,
      mockUserAnswersRepository
    )

    // Sane defaults for anything that constructs an AuthenticatedIdentifierAction directly from mockAppConfig.
    Mockito.when(mockAppConfig.manageIsaEnrolmentKey).thenReturn("HMRC-DISA-ORG")
    Mockito.when(mockAppConfig.zrefIdentifierKey).thenReturn("ZREF")
    Mockito.when(mockAppConfig.loginUrl).thenReturn(authLoginStubSignInEndpoint)
    Mockito
      .when(mockAppConfig.loginContinueUrl)
      .thenReturn(loginContinueEndpoint)
  }

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .configure("http-verbs.retries.intervals" -> Seq("1ms", "1ms", "1ms"))
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AppConfig].toInstance(mockAppConfig),
      bind[RegistrationConnector].toInstance(mockRegistrationConnector),
      bind[UserAnswersRepository].toInstance(mockUserAnswersRepository),
      bind[IdentifierAction].to[AuthenticatedIdentifierAction]
    )
    .build()

  // For controller specs: builds an Application with fake identify/getData actions so controller
  // behaviour can be tested via route() without re-exercising real auth/data-retrieval logic
  // (that's covered by IdentifierActionSpec/DataRetrievalActionSpec instead).
  def applicationBuilder(
    effectiveAnswers: Answers = Answers(),
    sessionAnswers: Option[UserAnswers] = None,
    zReference: String = testZref,
    credentialId: String = testCredentialId,
    sessionId: String = testSessionId
  ): GuiceApplicationBuilder = {
    val bodyParsers = stubControllerComponents().parsers
    GuiceApplicationBuilder()
      .configure("play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck")
      .overrides(
        bind[IdentifierAction].toInstance(new FakeIdentifierAction(bodyParsers, zReference, credentialId, sessionId)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(effectiveAnswers, sessionAnswers)),
        bind[UserAnswersRepository].toInstance(mockUserAnswersRepository)
      )
  }

  implicit def messages(implicit app: Application): Messages =
    app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  def messages(key: String)(implicit app: Application): String = messages(app).messages(key)
}
