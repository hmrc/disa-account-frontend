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

package models.requests

import play.api.test.FakeRequest
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import utils.BaseUnitSpec

class DataRequestSpec extends BaseUnitSpec {

  private def request(
    email: Option[String],
    effectiveAnswers: Answers = Answers(signatories = Some(testSignatories))
  ): DataRequest[_] =
    DataRequest(
      FakeRequest(),
      testZref,
      testCredentialId,
      loggedInEmail = email,
      sessionId = testSessionId,
      sessionAnswers = None,
      originalAnswers = Answers(signatories = Some(testSignatories)),
      effectiveAnswers = effectiveAnswers
    )

  "DataRequest.isSignatory" should {

    "return true when the authenticated email matches a signatory ignoring case and surrounding whitespace" in {
      request(Some(s"  ${testSignatoryEmail.toUpperCase}  ")).isSignatory shouldBe true
    }

    "continue using the original signatories after the user removes themselves from the effective answers" in {
      request(Some(testSignatoryEmail), effectiveAnswers = Answers()).isSignatory shouldBe true
    }

    "return false when the authenticated email does not match a signatory" in {
      request(Some("someone.else@example.com")).isSignatory shouldBe false
    }

    "return false when the authenticated email is missing or blank" in {
      request(None).isSignatory       shouldBe false
      request(Some("   ")).isSignatory shouldBe false
    }
  }
}
