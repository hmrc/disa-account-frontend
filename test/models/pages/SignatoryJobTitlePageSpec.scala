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
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryJobTitlePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

class SignatoryJobTitlePageSpec extends BaseUnitSpec {

  "SignatoryJobTitlePage" should {

    "update the matching signatory's job title and preserve their name, retaining other session updates" in {
      val existingUpdates   = SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      val existingSignatory = Signatory(testSignatoryId, fullName = Some(testSignatoryName))
      val request           = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        Some(UserAnswers(testSessionId, existingUpdates)),
        Answers(signatories = Some(Seq(existingSignatory)))
      )

      SignatoryJobTitlePage(testSignatoryId).saveAnswerAndHandleDependents(request, testSignatoryJobTitle) shouldBe
        existingUpdates.copy(
          signatories = Assign(Seq(existingSignatory.copy(jobTitle = Some(testSignatoryJobTitle))))
        )
    }

    "update only the matching signatory while preserving other signatories already in the effective answers" in {
      val otherSignatory    = Signatory("signatory-2", fullName = Some("Other Signatory"), jobTitle = Some("Manager"))
      val existingSignatory = Signatory(testSignatoryId, fullName = Some(testSignatoryName))
      val request           = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        None,
        Answers(signatories = Some(Seq(existingSignatory, otherSignatory)))
      )

      SignatoryJobTitlePage(testSignatoryId).saveAnswerAndHandleDependents(request, testSignatoryJobTitle) shouldBe
        SessionUpdates(
          signatories = Assign(Seq(existingSignatory.copy(jobTitle = Some(testSignatoryJobTitle)), otherSignatory))
        )
    }

    "leave the signatories unchanged when the id does not match an existing signatory" in {
      val existingSignatory = Signatory(testSignatoryId, fullName = Some(testSignatoryName))
      val request           = DataRequest(
        FakeRequest(),
        testZref,
        testCredentialId,
        testSessionId,
        None,
        Answers(signatories = Some(Seq(existingSignatory)))
      )

      SignatoryJobTitlePage("some-other-id").saveAnswerAndHandleDependents(request, testSignatoryJobTitle) shouldBe
        SessionUpdates(signatories = Assign(Seq(existingSignatory)))
    }
  }
}
