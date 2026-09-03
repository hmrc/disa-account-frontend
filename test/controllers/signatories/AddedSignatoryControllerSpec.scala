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

package controllers.signatories

import org.jsoup.Jsoup
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.signatories.Signatory
import utils.BaseUnitSpec

class AddedSignatoryControllerSpec extends BaseUnitSpec {

  private val completeSignatory =
    Signatory(testSignatoryId, Some(testSignatoryName), Some(testSignatoryJobTitle))

  private def completeSignatories(count: Int): Seq[Signatory] =
    (1 to count).map(number => Signatory(s"signatory-$number", Some(s"Signatory $number"), Some("Director")))

  "AddedSignatoryController.onPageLoad" should {

    "render one signatory with Change and the temporary Remove fallback" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(completeSignatory)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedSignatoriesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)      shouldBe OK
        doc.title()         shouldBe "You currently have a signatory - Manage ISAs - GOV.UK"
        doc.select("h1").text() shouldBe "You currently have a signatory"
        doc.select(".govuk-caption-l").isEmpty shouldBe true
        doc.text() should include("Add or remove signatories, but you must have at least 1 and no more than 25.")
        doc.text() should include(testSignatoryName)

        val actions = doc.select(".govuk-summary-list__actions a")
        actions.size()             shouldBe 2
        actions.get(0).attr("href") shouldBe s"$checkSignatoryDetailsEndpoint?id=$testSignatoryId"
        actions.get(0).text()       shouldBe s"Change $testSignatoryName details"
        actions.get(1).attr("href") shouldBe changeOfCircumstancesEndpoint
        actions.get(1).text()       shouldBe s"Remove $testSignatoryName details"

        doc.select("input[type=radio][name=value]").size() shouldBe 2
        doc.select("input[type=radio][checked]").isEmpty shouldBe true
        doc.select(".govuk-radios--inline").size() shouldBe 1
        doc.select("button.govuk-button").text() shouldBe "Continue"
      }
    }

    "render the plural heading for multiple signatories" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(completeSignatories(2)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedSignatoriesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)           shouldBe OK
        doc.select("h1").text() shouldBe "You have 2 signatories"
        doc.select(".govuk-summary-list__row").size() shouldBe 2
      }
    }

    "render the maximum state without the add-another question" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(completeSignatories(25)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedSignatoriesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)           shouldBe OK
        doc.select("h1").text() shouldBe "You have 25 signatories"
        doc.text() should include("You must have at least one signatory. The maximum is 25.")
        doc.text() should not include "Do you want to add another signatory?"
        doc.select("input[type=radio]").isEmpty shouldBe true
      }
    }

    "defensively use the maximum state above 25 signatories" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(completeSignatories(26)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedSignatoriesEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)           shouldBe OK
        doc.select("h1").text() shouldBe "You have 26 signatories"
        doc.select("input[type=radio]").isEmpty shouldBe true
      }
    }

    "redirect when signatories are missing, empty or incomplete" in {
      val answerSets = Seq(
        Answers(),
        Answers(signatories = Some(Seq.empty)),
        Answers(signatories = Some(Seq(completeSignatory.copy(jobTitle = None))))
      )

      answerSets.foreach { answers =>
        val application = applicationBuilder(effectiveAnswers = answers).build()

        running(application) {
          val result = route(application, FakeRequest(GET, addedSignatoriesEndpoint)).value

          status(result)                 shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        }
      }
    }
  }

  "AddedSignatoryController.onSubmit" should {

    "redirect Yes to the signatory name page without persisting the answer" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(completeSignatory)))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedSignatoriesEndpoint)
          .withFormUrlEncodedBody("value" -> "yes")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe signatoryNameEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect No to change of circumstances without persisting the answer" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(completeSignatory)))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedSignatoriesEndpoint)
          .withFormUrlEncodedBody("value" -> "no")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "show the required inline error and error summary when no option is selected" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(Seq(completeSignatory)))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedSignatoriesEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.select(".govuk-error-summary").text() should include("Select yes if you’d like to add another signatory")
        doc.select(".govuk-error-message").text() should include("Select yes if you’d like to add another signatory")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect from the maximum state without validating or persisting a radio answer" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(signatories = Some(completeSignatories(25)))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedSignatoriesEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
