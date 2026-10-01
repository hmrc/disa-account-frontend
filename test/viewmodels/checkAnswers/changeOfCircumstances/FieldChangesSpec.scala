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

package viewmodels.checkAnswers.changeOfCircumstances

import uk.gov.hmrc.disaaccountfrontend.models.{Answers, CorrespondenceAddress}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances.FieldChanges
import utils.BaseUnitSpec

class FieldChangesSpec extends BaseUnitSpec {

  "FieldChanges.rows" should {

    "return no rows when nothing changed" in {
      val answers = Answers(tradingName = Some("ABC Bank"), organisationTelephoneNumber = Some("123456789"))

      FieldChanges.rows(answers, answers)(messages(app)) shouldBe empty
    }

    "show the trading name change" in {
      val original  = Answers(tradingName = Some("ABC Bank"))
      val effective = Answers(tradingName = Some("XYZ Bank"))

      val rows = FieldChanges.rows(original, effective)(messages(app))

      rows.map(_.key.content.asHtml.body)   shouldBe Seq("Changed trading name")
      rows.map(_.value.content.asHtml.body) shouldBe Seq("ABC Bank to XYZ Bank")
    }

    "show the organisation telephone number change" in {
      val original  = Answers(organisationTelephoneNumber = Some("111"))
      val effective = Answers(organisationTelephoneNumber = Some("222"))

      val rows = FieldChanges.rows(original, effective)(messages(app))

      rows.map(_.key.content.asHtml.body)   shouldBe Seq("Changed organisation telephone number")
      rows.map(_.value.content.asHtml.body) shouldBe Seq("111 to 222")
    }

    "show the organisation email change" in {
      val original  = Answers(organisationEmailAddress = Some("old@example.com"))
      val effective = Answers(organisationEmailAddress = Some("new@example.com"))

      val rows = FieldChanges.rows(original, effective)(messages(app))

      rows.map(_.key.content.asHtml.body)   shouldBe Seq("Changed organisation email")
      rows.map(_.value.content.asHtml.body) shouldBe Seq("old@example.com to new@example.com")
    }

    "show the correspondence address change with each address line on its own line" in {
      val original  = Answers(
        correspondenceAddress =
          Some(CorrespondenceAddress(Some("1 Old Road"), Some("Old Town"), postCode = Some("AB1 1AB")))
      )
      val effective = Answers(
        correspondenceAddress =
          Some(CorrespondenceAddress(Some("2 New Road"), Some("New Town"), postCode = Some("CD2 2CD")))
      )

      val rows = FieldChanges.rows(original, effective)(messages(app))

      rows.map(_.key.content.asHtml.body)   shouldBe Seq("Changed correspondence address")
      rows.map(_.value.content.asHtml.body) shouldBe Seq(
        "1 Old Road<br>Old Town<br>AB1 1AB<br>to<br>2 New Road<br>New Town<br>CD2 2CD"
      )
    }

    "not show an address row when the address did not change" in {
      val address = CorrespondenceAddress(Some("1 Test Road"), postCode = Some("AB1 1AB"))
      val answers = Answers(correspondenceAddress = Some(address))

      FieldChanges.rows(answers, answers)(messages(app)) shouldBe empty
    }

    "not show a row when a field was cleared" in {
      val original  = Answers(tradingName = Some("ABC Bank"))
      val effective = Answers(tradingName = None)

      FieldChanges.rows(original, effective)(messages(app)) shouldBe empty
    }

    "show every changed field together" in {
      val original  = Answers(
        tradingName = Some("ABC Bank"),
        organisationTelephoneNumber = Some("111"),
        organisationEmailAddress = Some("old@example.com")
      )
      val effective = Answers(
        tradingName = Some("XYZ Bank"),
        organisationTelephoneNumber = Some("222"),
        organisationEmailAddress = Some("new@example.com")
      )

      val rows = FieldChanges.rows(original, effective)(messages(app))

      rows.map(_.key.content.asHtml.body) shouldBe Seq(
        "Changed trading name",
        "Changed organisation telephone number",
        "Changed organisation email"
      )
    }
  }
}
