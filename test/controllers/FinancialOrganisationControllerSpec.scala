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

package controllers

import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation
import uk.gov.hmrc.disaaccountfrontend.models.certificatesofauthority.FinancialOrganisation.{Bank, BuildingSociety}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class FinancialOrganisationControllerSpec extends BaseUnitSpec {

  private def checkboxIsChecked(html: String, organisation: FinancialOrganisation): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=${organisation.toString}]").hasAttr("checked")

  "FinancialOrganisationController.onPageLoad" should {

    "render the agreed page content and prefill effective answers" in {
      val application = applicationBuilder(
        effectiveAnswers = Answers(financialOrganisation = Some(Seq(BuildingSociety, Bank)))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, financialOrganisationEndpoint)).value
        val html   = contentAsString(result)
        val doc    = Jsoup.parse(html)

        status(result)                               shouldBe OK
        doc.title()                                  shouldBe "Financial organisation - Manage ISAs - GOV.UK"
        doc.select("h1").text()                      shouldBe "Financial organisation"
        doc.select(".govuk-caption-l").isEmpty       shouldBe true
        doc.select(".govuk-fieldset__legend").text() shouldBe "Which best describes your organisation?"
        doc.select(".govuk-hint").text()             shouldBe "Select all that apply"
        doc.select("button.govuk-button").text()     shouldBe "Continue"
        FinancialOrganisation.values.foreach { organisation =>
          html should include(messages(s"financialOrganisation.${organisation.toString}")(application))
        }
        checkboxIsChecked(html, BuildingSociety)     shouldBe true
        checkboxIsChecked(html, Bank)                shouldBe true
      }
    }

    "render an empty form when there is no effective answer" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, financialOrganisationEndpoint)).value
        val html   = contentAsString(result)

        status(result) shouldBe OK
        FinancialOrganisation.values.foreach { organisation =>
          checkboxIsChecked(html, organisation) shouldBe false
        }
      }
    }

  }

  "FinancialOrganisationController.onSubmit" should {

    "save selections in display order, preserve existing updates and redirect to Change of Circumstances" in {
      val existingUpdates = SessionUpdates(organisationTelephoneNumber = Assign(testOrgTelephoneNumber))
      val existingAnswers = UserAnswers(testSessionId, existingUpdates)
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = applicationBuilder(
        effectiveAnswers = Answers(organisationTelephoneNumber = Some(testOrgTelephoneNumber)),
        sessionAnswers = Some(existingAnswers)
      ).build()

      running(application) {
        val request = FakeRequest(POST, financialOrganisationEndpoint)
          .withFormUrlEncodedBody(
            "value[4]" -> Bank.toString,
            "value[3]" -> BuildingSociety.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")

        val result = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe existingUpdates.copy(
          financialOrganisation = Assign(Seq(BuildingSociety, Bank))
        )
      }
    }

    "return Bad Request with the exact error when no organisation type is selected" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, financialOrganisationEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text()           should include(
          "You need to tell us which organisation type you are. Select from the options"
        )
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.title()                                         should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

  }
}
