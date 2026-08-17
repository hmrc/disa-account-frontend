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

package uk.gov.hmrc.disaaccountfrontend.connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationConnector @Inject() (
  http: HttpClientV2,
  appConfig: AppConfig,
  protected val configuration: Config,
  protected val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends Retries
    with Logging {

  private val retryCondition: PartialFunction[Exception, Boolean] = {
    case UpstreamErrorResponse.Upstream5xxResponse(_) => true
  }

  def getRegistrationDetails(zref: String)(implicit hc: HeaderCarrier): Future[Option[RegistrationDetails]] = {
    val url = s"${appConfig.disaAccountBaseUrl}/disa-account/registration/$zref"
    retryFor[RegistrationDetails]("get disa-account registration details")(retryCondition) {
      http
        .get(url"$url")
        .execute[Either[UpstreamErrorResponse, RegistrationDetails]]
        .flatMap {
          case Right(details) => Future.successful(details)
          case Left(error)    => Future.failed(error)
        }
    }.map(Some(_))
      .recover { case err: UpstreamErrorResponse if err.statusCode == 404 =>
        logger.info(
          s"[RegistrationConnector][getRegistrationDetails] No registration details found in disa-account for zref: [$zref]"
        )
        None
      }
  }
}
