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

package uk.gov.hmrc.disaaccountfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (config: Configuration, servicesConfig: ServicesConfig) {

  val welshLanguageSupportEnabled: Boolean =
    config.getOptional[Boolean]("features.welsh-language-support").getOrElse(false)

  lazy val disaAccountBaseUrl: String = servicesConfig.baseUrl(serviceName = "disa-account")

  lazy val emailVerificationBaseUrl: String = servicesConfig.baseUrl(serviceName = "email-verification")

  val loginUrl: String         = config.get[String]("urls.login")
  val loginContinueUrl: String = config.get[String]("urls.loginContinue")
  val signOutUrl: String       = config.get[String]("urls.signOut")

  val timeout: Int   = config.get[Int]("timeout-dialog.timeout")
  val countdown: Int = config.get[Int]("timeout-dialog.countdown")

  val manageIsaEnrolmentKey: String  = config.get[String]("enrolments.manageIsa")
  val zrefIdentifierKey: String      = config.get[String]("enrolments.zrefIdentifierKey")
  val p2pLoansInformationUrl: String = config.get[String]("urls.external.p2pLoansInformation")

  val cacheTtl: Long = config.get[Long]("cache.ttlInSeconds")

  val maxSignatories: Int = config.get[Int]("max-signatories")
  val maxLiaisonOfficers: Int = config.get[Int]("max-liaison-officers")
}
