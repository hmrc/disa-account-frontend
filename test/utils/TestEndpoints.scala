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

trait TestEndpoints {

  val accountFrontendRoutePrefix: String = "/obligations/account/isa"

  val enterYourOrganisationAddressEndpoint: String =
    s"$accountFrontendRoutePrefix/enter-your-organisation-address"
  val organisationTelephoneNumberEndpoint: String  =
    s"$accountFrontendRoutePrefix/organisation-telephone-number"
  val tradingNameEndpoint: String                  = s"$accountFrontendRoutePrefix/trading-name"
  val innovativeFinancialProductsEndpoint: String  =
    s"$accountFrontendRoutePrefix/innovative-financial-products"
  val peerToPeerPlatformEndpoint: String           = s"$accountFrontendRoutePrefix/peer-to-peer-loans"
  val organisationEmailAddressEndpoint: String     = s"$accountFrontendRoutePrefix/organisation-email-address"
  val emailVerificationCodeEndpoint: String        = s"$accountFrontendRoutePrefix/email-verification-code"
  val requestNewCodeEndpoint: String               = s"$accountFrontendRoutePrefix/request-new-code"
  val financialOrganisationEndpoint: String        = s"$accountFrontendRoutePrefix/financial-organisation"
  val liaisonOfficerNameEndpoint: String           = s"$accountFrontendRoutePrefix/liaison-officer-name"
  val changeOfCircumstancesEndpoint: String        = s"$accountFrontendRoutePrefix/change-of-circumstances"
  val fcaArticlesEndpoint: String                  = s"$accountFrontendRoutePrefix/fca-articles"
  val keepAliveEndpoint: String                    = s"$accountFrontendRoutePrefix/refresh-session"
  val signOutEndpoint: String                      = s"$accountFrontendRoutePrefix/sign-out"
  val signedOutEndpoint: String                    = s"$accountFrontendRoutePrefix/signed-out"
  val customRedirectEndpoint: String               = "/custom-redirect"

  val authLoginStubSignInEndpoint: String = "http://localhost:9949/auth-login-stub/gg-sign-in"
  val loginContinueEndpoint: String       =
    "http://localhost:12107/disa-account-frontend/enter-your-organisation-address"
  val basGatewaySignOutEndpoint: String   = "http://localhost:9553/bas-gateway/sign-out-without-state"
  val disaAccountBaseUrl: String          = "http://localhost:12105"
  val emailVerificationBaseUrl: String    = "http://localhost:9891"

  def disaAccountRegistrationEndpoint(zReference: String): String =
    s"$disaAccountBaseUrl/disa-account/registration/$zReference"

  val emailVerificationSendCodeEndpoint: String =
    s"$emailVerificationBaseUrl/email-verification/v2/send-code"

  val emailVerificationVerifyCodeEndpoint: String =
    s"$emailVerificationBaseUrl/email-verification/v2/verify-code"

  def liaisonOfficerNameEndpointFor(id: String): String = s"$liaisonOfficerNameEndpoint?id=$id"
}
