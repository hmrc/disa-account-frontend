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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.test.Helpers.await
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import uk.gov.hmrc.disaaccountfrontend.utils.BaseIntegrationSpec
import uk.gov.hmrc.disaaccountfrontend.utils.WiremockHelper.stubGet
import uk.gov.hmrc.http.UpstreamErrorResponse

class RegistrationConnectorISpec extends BaseIntegrationSpec {

  val connector: RegistrationConnector = app.injector.instanceOf[RegistrationConnector]

  val registrationUrl: String = s"/disa-account/registration/$testZref"

  "RegistrationConnector.getRegistrationDetails" should {

    "return Some(RegistrationDetails) when the backend returns 200 OK with valid json" in {
      val responseBody =
        s"""{
           |  "groupId": "test-group-id",
           |  "organisationDetails": {
           |    "correspondenceAddress": ${Json.toJson(testCorrespondenceAddress)},
           |    "orgTelephoneNumber": "$testOrgTelephoneNumber"
           |  }
           |}""".stripMargin
      stubGet(registrationUrl, OK, responseBody)

      val result: Option[RegistrationDetails] = await(connector.getRegistrationDetails(testZref))

      result shouldBe Some(testRegistrationDetails)
    }

    "return None when the backend returns a 404" in {
      stubGet(registrationUrl, NOT_FOUND, """{"statusCode":404,"message":"Not found"}""")

      val result: Option[RegistrationDetails] = await(connector.getRegistrationDetails(testZref))

      result shouldBe None
    }

    "propagate the failure when the backend returns an unexpected error, so the journey is not entered" in {
      stubGet(registrationUrl, INTERNAL_SERVER_ERROR, """{"statusCode":500,"message":"Boom"}""")

      val result = await(connector.getRegistrationDetails(testZref).failed)

      result shouldBe an[UpstreamErrorResponse]
    }
  }
}
