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

package models.pages

import play.api.test.FakeRequest
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryNamePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class SignatoryNamePageSpec extends BaseUnitSpec {

  "SignatoryNamePage" should {

    "add a new signatory to an empty list and preserve existing session updates" in {
      val existingUpdates = SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      val request         = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        Some(UserAnswers(testSessionId, existingUpdates)),
        Answers()
      )

      SignatoryNamePage(testSignatoryId).saveAnswerAndHandleDependents(request, testSignatoryName) shouldBe
        existingUpdates.copy(signatories =
          Assign(Signatories(Seq(Signatory(testSignatoryId, fullName = Some(testSignatoryName)))))
        )
    }

    "append a new signatory while preserving other signatories already in the effective answers" in {
      val request = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        None,
        Answers(signatories = Some(testSignatories))
      )

      val newId = "signatory-2"

      SignatoryNamePage(newId).saveAnswerAndHandleDependents(request, "New Signatory") shouldBe
        SessionUpdates(signatories =
          Assign(Signatories(testSignatories.signatories :+ Signatory(newId, fullName = Some("New Signatory"))))
        )
    }

    "update the matching signatory's name and preserve their job title" in {
      val existingSignatory = Signatory(testSignatoryId, fullName = Some("Old Name"), jobTitle = Some("Director"))
      val request           = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        None,
        Answers(signatories = Some(Signatories(Seq(existingSignatory))))
      )

      SignatoryNamePage(testSignatoryId).saveAnswerAndHandleDependents(request, testSignatoryName) shouldBe
        SessionUpdates(signatories =
          Assign(Signatories(Seq(existingSignatory.copy(fullName = Some(testSignatoryName)))))
        )
    }
  }
}
