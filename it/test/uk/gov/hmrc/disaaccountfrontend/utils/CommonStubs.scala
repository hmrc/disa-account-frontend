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

package uk.gov.hmrc.disaaccountfrontend.utils

import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubPost

trait CommonStubs {

  val testHeaders: Seq[(String, String)] = Seq("Authorization" -> "mock-bearer-token")

  def stubAuth(zref: String, credentialId: String): Unit =
    stubPost(
      url = "/auth/authorise",
      status = OK,
      responseBody = Json
        .obj(
          "optionalCredentials"  -> Json.obj(
            "providerId"   -> credentialId,
            "providerType" -> "GovernmentGateway"
          ),
          "authorisedEnrolments" -> Json.arr(
            Json.obj(
              "key"         -> "HMRC-DISA-ORG",
              "identifiers" -> Json.arr(
                Json.obj("key" -> "ZREF", "value" -> zref)
              ),
              "state"       -> "Activated"
            )
          )
        )
        .toString()
    )

  def stubAuthFail(): Unit = stubPost(url = "/auth/authorise", status = UNAUTHORIZED, responseBody = "{}")
}
