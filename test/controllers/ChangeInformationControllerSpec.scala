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

import controllers.actions.FakeAccountMaintenanceGuardAction
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.test.Helpers.*
import play.api.test.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Unchanged}
import uk.gov.hmrc.disaaccountfrontend.models.ChangeInformationSelection.*
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future
import scala.jdk.CollectionConverters.*

class ChangeInformationControllerSpec extends BaseUnitSpec {

  private def signatoryApplicationBuilder(sessionAnswers: Option[UserAnswers] = None) =
    applicationBuilder(
      effectiveAnswers = Answers(signatories = Some(testSignatories)),
      sessionAnswers = sessionAnswers,
      email = Some(testSignatoryEmail)
    )

  private def checkboxIsChecked(html: String, value: String): Boolean =
    Jsoup.parse(html).select(s"input.govuk-checkboxes__input[value=$value]").hasAttr("checked")

  "ChangeInformationController.onPageLoad" should {

    "render the change information options in order" in {
      val application = signatoryApplicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeInformationEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                                             shouldBe OK
        doc.title()                                                                  should startWith("What would you like to change?")
        doc.select("h1").text()                                                    shouldBe "What would you like to change?"
        doc.select(".govuk-hint").text()                                           shouldBe "Select all sections that apply"
        doc.select(".govuk-checkboxes__label").eachText().asScala.toSeq            shouldBe Seq(
          "Organisation information",
          "ISA Product information",
          "Authorised users",
          "View all information"
        )
        doc.select(".govuk-checkboxes__divider").text()                            shouldBe "or"
        doc.select("button.govuk-button").text()                                   shouldBe "Continue"
        doc.select("input.govuk-checkboxes__input").eachAttr("name").asScala.toSet shouldBe Set("value[]")
        doc.select("input[data-behaviour=exclusive]").attr("value")                shouldBe "viewAllInformation"
      }
    }

    "hide ISA product information when the user is not a signatory" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeInformationEndpoint)).value
        val labels = Jsoup.parse(contentAsString(result)).select(".govuk-checkboxes__label").eachText().asScala.toSeq

        status(result) shouldBe OK
        labels         shouldBe Seq("Organisation information", "Authorised users", "View all information")
      }
    }

    "redirect to manage ISAs when an ISA product change is under review" in {
      val application = applicationBuilder(
        accountMaintenanceGuard = new FakeAccountMaintenanceGuardAction(blocked = true)
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeInformationEndpoint)).value

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe manageIsasEndpoint
      }
    }

    "prefill selections saved in the session" in {
      val sessionAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(
          changeInformationSelections = Assign(Seq(OrganisationInformation, IsaProductInformation, AuthorisedUsers))
        )
      )
      val application    = applicationBuilder(sessionAnswers = Some(sessionAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeInformationEndpoint)).value
        val html   = contentAsString(result)

        status(result)                                            shouldBe OK
        checkboxIsChecked(html, OrganisationInformation.toString) shouldBe true
        checkboxIsChecked(html, IsaProductInformation.toString)   shouldBe false
        checkboxIsChecked(html, AuthorisedUsers.toString)         shouldBe true
        checkboxIsChecked(html, viewAllInformationFormValue)      shouldBe false
      }
    }

    "prefill view all information when it was saved in the session" in {
      val sessionAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(changeInformationSelections = Assign(Seq(ViewAllInformation)))
      )
      val application    = applicationBuilder(sessionAnswers = Some(sessionAnswers)).build()

      running(application) {
        val result = route(application, FakeRequest(GET, changeInformationEndpoint)).value
        val html   = contentAsString(result)

        status(result)                                            shouldBe OK
        checkboxIsChecked(html, OrganisationInformation.toString) shouldBe false
        checkboxIsChecked(html, AuthorisedUsers.toString)         shouldBe false
        checkboxIsChecked(html, viewAllInformationFormValue)      shouldBe true
      }
    }
  }

  "ChangeInformationController.onSubmit" should {

    "show an error when no option is selected" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value
        val doc     = Jsoup.parse(contentAsString(result))

        status(result)                                    shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text()           should include(
          "Select from the options what you would like to change"
        )
        doc.select(".govuk-error-summary a").text()       shouldBe
          "Select from the options what you would like to change"
        doc.select(".govuk-error-summary a").attr("href") shouldBe "#value_0"
        doc.title()                                         should startWith("Error:")
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "store only view all information and redirect when it is submitted with another selection" in {
      val existingUpdates = SessionUpdates(tradingName = Assign("Existing trading name"))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> "organisationInformation",
            "value[]" -> "viewAllInformation"
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          changeInformationSelections = Assign(Seq(ViewAllInformation))
        )
      }
    }

    "store view all information when a non-signatory submits it" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody("value[]" -> viewAllInformationFormValue)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates.changeInformationSelections shouldBe Assign(
          Seq(ViewAllInformation)
        )
      }
    }

    "reject ISA product information when submitted by a non-signatory" in {
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody("value[]" -> IsaProductInformation.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe BAD_REQUEST
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "store selected options in display order" in {
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application = applicationBuilder().build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> AuthorisedUsers.toString,
            "value[]" -> OrganisationInformation.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates.changeInformationSelections shouldBe Assign(
          Seq(OrganisationInformation, AuthorisedUsers)
        )
      }
    }

    "clear authorised users answers when the section is deselected after previously viewing all information" in {
      val existingUpdates = SessionUpdates(
        changeInformationSelections = Assign(Seq(ViewAllInformation)),
        tradingName = Assign("Existing trading name"),
        signatories = Assign(testSignatories),
        liaisonOfficers = Assign(testLiaisonOfficers)
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody("value[]" -> OrganisationInformation.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          changeInformationSelections = Assign(Seq(OrganisationInformation)),
          signatories = Unchanged,
          liaisonOfficers = Unchanged
        )
      }
    }

    "keep authorised users answers when the section remains selected" in {
      val existingUpdates = SessionUpdates(
        changeInformationSelections = Assign(Seq(AuthorisedUsers)),
        signatories = Assign(testSignatories),
        liaisonOfficers = Assign(testLiaisonOfficers)
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody(
            "value[]" -> OrganisationInformation.toString,
            "value[]" -> AuthorisedUsers.toString
          )
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          changeInformationSelections = Assign(Seq(OrganisationInformation, AuthorisedUsers))
        )
      }
    }

    "clear organisation and ISA product answers when their sections are deselected" in {
      val existingUpdates = SessionUpdates(
        changeInformationSelections = Assign(Seq(ViewAllInformation)),
        tradingName = Assign("Existing trading name"),
        organisationTelephoneNumber = Assign("0123456789"),
        isaProducts = Assign(Seq(uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.CashIsas)),
        fcaArticles = Assign(Seq(uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles.FcaArticle14)),
        signatories = Assign(testSignatories)
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody("value[]" -> AuthorisedUsers.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          changeInformationSelections = Assign(Seq(AuthorisedUsers)),
          tradingName = Unchanged,
          organisationTelephoneNumber = Unchanged,
          isaProducts = Unchanged,
          fcaArticles = Unchanged
        )
      }
    }

    "not clear any answers on the first ever selection, even where a section is left out" in {
      val existingUpdates = SessionUpdates(signatories = Assign(testSignatories))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val application     = signatoryApplicationBuilder(
        sessionAnswers = Some(UserAnswers(testSessionId, existingUpdates))
      ).build()

      running(application) {
        val request = FakeRequest(POST, changeInformationEndpoint)
          .withFormUrlEncodedBody("value[]" -> OrganisationInformation.toString)
          .withHeaders("Csrf-Token" -> "nocheck")
        val result  = route(application, request).value

        status(result) shouldBe SEE_OTHER

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.updates shouldBe existingUpdates.copy(
          changeInformationSelections = Assign(Seq(OrganisationInformation))
        )
      }
    }
  }
}
