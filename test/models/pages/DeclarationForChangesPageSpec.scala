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

import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.pages.DeclarationForChangesPage
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import utils.BaseUnitSpec

class DeclarationForChangesPageSpec extends BaseUnitSpec {

  private val completeSignatory = Signatory("signatory-1", Some("Jane Doe"), Some("Director"))
  private val completeOfficer   = LiaisonOfficer(
    "liaison-officer-1",
    Some("John Smith"),
    Some("01234567890"),
    Set(LiaisonOfficerCommunication.values.head),
    Some("john@example.com")
  )

  "DeclarationForChangesPage" should {

    "allow access when a complete signatory and liaison officer exist" in {
      val answers = Answers(
        signatories = Some(Signatories(Seq(completeSignatory))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer)))
      )

      DeclarationForChangesPage.canBeAccessedWith(answers) shouldBe true
    }

    "deny access when no complete signatory exists" in {
      val answers = Answers(
        signatories = Some(Signatories(Seq(Signatory("incomplete", Some("Partial"))))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer)))
      )

      DeclarationForChangesPage.canBeAccessedWith(answers) shouldBe false
    }

    "deny access when no complete liaison officer exists" in {
      val answers = Answers(
        signatories = Some(Signatories(Seq(completeSignatory))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(LiaisonOfficer("incomplete", Some("Partial")))))
      )

      DeclarationForChangesPage.canBeAccessedWith(answers) shouldBe false
    }
  }
}
