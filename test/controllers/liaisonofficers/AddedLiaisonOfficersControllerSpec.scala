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

package controllers.liaisonofficers

import org.jsoup.Jsoup
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficerCommunication.ByPost
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import utils.BaseUnitSpec

class AddedLiaisonOfficersControllerSpec extends BaseUnitSpec {

  private val completeOfficer =
    LiaisonOfficer(
      testLiaisonOfficerId,
      Some(testName),
      Some("07777777777"),
      Set(ByPost),
      Some("jane.smith@example.com")
    )

  private def completedOfficers(count: Int): Seq[LiaisonOfficer] =
    (1 to count).map(number =>
      LiaisonOfficer(
        s"officer-$number",
        Some(s"Officer $number"),
        Some("Phone Number"),
        Set(ByPost),
        Some(s"$number@test.com")
      )
    )

  "AddedLiaisonOfficersController.onPageLoad" should {
    "render one officer with Change and Remove actions" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer))))
      ).build()
      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                         shouldBe OK
        doc.title()                            shouldBe "You currently have a liaison officer - Manage ISAs - GOV.UK"
        doc.select("h1").text()                shouldBe "You currently have a liaison officer"
        doc.select(".govuk-caption-l").isEmpty shouldBe true
        doc.text()                               should include("Add or remove liaison officers, but you must have at least 1 and no more than 15.")
        doc.text()                               should include(testName)

        val actions = doc.select(".govuk-summary-list__actions a")
        actions.size()              shouldBe 2
        actions.get(0).attr("href") shouldBe s"$checkLiaisonOfficerDetailsEndpoint?id=$testLiaisonOfficerId"
        actions.get(0).text()       shouldBe s"Change $testName details"
        actions.get(1).attr("href") shouldBe s"$removeLiaisonOfficerEndpoint?id=$testLiaisonOfficerId"
        actions.get(1).text()       shouldBe s"Remove $testName details"

        doc.select("input[type=radio][name=value]").size() shouldBe 2
        doc.select("input[type=radio][checked]").isEmpty   shouldBe true
        doc.select(".govuk-radios--inline").size()         shouldBe 1
        doc.select("button.govuk-button").text()           shouldBe "Continue"
      }
    }

    "render the plural heading for multiple officers" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(completedOfficers(2))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                shouldBe OK
        doc.select("h1").text()                       shouldBe "You have 2 liaison officers"
        doc.select(".govuk-summary-list__row").size() shouldBe 2
      }
    }

    "exclude incomplete officers from the list and heading count" in {
      val incompleteOfficer = LiaisonOfficer("incomplete-id", Some("Incomplete officer"), None)
      val application       = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(completedOfficers(2) :+ incompleteOfficer)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                shouldBe OK
        doc.select("h1").text()                       shouldBe "You have 2 liaison officers"
        doc.select(".govuk-summary-list__row").size() shouldBe 2
        doc.text()                                      should not include "Incomplete Officer"
      }
    }

    "allow another officers when incomplete records bring the stored collection to the maximum" in {
      val officers = completedOfficers(14) :+ LiaisonOfficer("incomplete-id", Some("Incomplete Officers"), None)
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(officers)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        doc.select("h1").text() shouldBe "You have 14 liaison officers"
        doc.select("input[type=radio]").size() shouldBe 2
      }
    }

    "render the maximum state without the add-another question" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(completedOfficers(15))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc = Jsoup.parse(contentAsString(result))

        status(result) shouldBe OK
        doc.select("h1").text() shouldBe "You have 15 liaison officers"
        doc.text() should include("You must have at least one liaison officer. The maximum is 15.")
        doc.text() should not include "Do you want to add another liaison officer?"
        doc.select("input[type=radio]").isEmpty shouldBe true
      }
    }

    "defensively use the maximum state above 15 officers" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(completedOfficers(16))))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe OK
        doc.select("h1").text()                 shouldBe "You have 16 liaison officers"
        doc.select("input[type=radio]").isEmpty shouldBe true
      }
    }

    "redirect when officers are missing, empty or incomplete" in {
      val answerSets = Seq(
        Answers(),
        Answers(liaisonOfficers = Some(LiaisonOfficers())),
        Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer.copy(email = None)))))
      )

      answerSets.foreach { answers =>
        val application = applicationBuilder(effectiveAnswers = answers).build()

        running(application) {
          val result = route(application, FakeRequest(GET, addedLiaisonOfficerEndpoint)).value

          status(result) shouldBe SEE_OTHER
          redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        }
      }
    }

  }


  "AddedLiaisonOfficersController.onSubmit" should {

    "redirect Yes to the officer name page without persisting the answer" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer))))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedLiaisonOfficerEndpoint)
          .withFormUrlEncodedBody("value" -> "yes")
          .withHeaders("Csrf-Token" -> "nocheck")
        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe liaisonOfficerNameEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "show the required inline error and error summary when no option is selected" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(Seq(completeOfficer))))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedLiaisonOfficerEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result = route(application, request).value
        val doc = Jsoup.parse(contentAsString(result))

        status(result) shouldBe BAD_REQUEST
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.select(".govuk-error-summary").text() should include("Select yes if you’d like to add another liaison officer")
        doc.select(".govuk-error-message").text() should include("Select yes if you’d like to add another liaison officer")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "redirect from the maximum state without validating or persisting a radio answer" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(liaisonOfficers = Some(LiaisonOfficers(completedOfficers(15))))
      ).build()

      running(application) {
        val request = FakeRequest(POST, addedLiaisonOfficerEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result = route(application, request).value

        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe changeOfCircumstancesEndpoint
        verify(mockUserAnswersRepository, never).set(any())
      }
    }



  }


}
