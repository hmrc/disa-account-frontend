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
import org.mockito.Mockito.{never, verify, when}
import play.api.Application
import play.api.test.*
import play.api.test.Helpers.*
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.CashIsas
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class PeerToPeerPlatformControllerSpec extends BaseUnitSpec {

  private val platformFieldName            = "value"
  private val csrfHeaderName               = "Csrf-Token"
  private val csrfHeaderValue              = "nocheck"
  private val titleMessageKey              = "peerToPeerPlatform.title"
  private val headingMessageKey            = "peerToPeerPlatform.heading"
  private val hintMessageKey               = "peerToPeerPlatform.details.summary"
  private val moreInfoMessageKey           = "peerToPeerPlatform.details.content.link"
  private val requiredErrorMessageKey      = "peerToPeerPlatform.error.required"
  private val isaProductsSectionCaptionKey = "sectionTitle.isaProducts"
  private val previousP2pPlatform          = "Old platform"

  private val eligibleAnswers = Answers(
    isaProducts = Some(Seq(CashIsas)),
    innovativeFinancialProducts = Some(Seq(PeertopeerLoansUsingAPlatformWith36hPermissions))
  )

  "PeerToPeerPlatformController.onPageLoad" should {

    "render the page and pre-populate a saved platform name" in {
      implicit val application: Application = applicationBuilder(
        effectiveAnswers = eligibleAnswers.copy(p2pPlatform = Some(testP2pPlatform))
      ).build()

      running(application) {
        val result = route(application, FakeRequest(GET, peerToPeerPlatformEndpoint)).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                                        shouldBe OK
        doc.title()                                             should include(messages(titleMessageKey))
        doc.select(s"input#$platformFieldName").attr("value") shouldBe testP2pPlatform
        doc.select("h1").text()                                 should include(messages(headingMessageKey))
        doc.text()                                              should include(messages(hintMessageKey))
        doc.text()                                              should include(messages(moreInfoMessageKey))
        doc.text()                                              should not include messages(isaProductsSectionCaptionKey)
      }
    }

    "redirect when the platform product is not selected" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, peerToPeerPlatformEndpoint)).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
      }
    }
  }

  "PeerToPeerPlatformController.onSubmit" should {

    "return the exact required error and not save when the platform name is blank" in {
      implicit val application: Application = applicationBuilder(effectiveAnswers = eligibleAnswers).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformEndpoint).withHeaders(csrfHeaderName -> csrfHeaderValue)
        ).value
        val doc    = Jsoup.parse(contentAsString(result))

        status(result)                          shouldBe BAD_REQUEST
        doc.select(".govuk-error-message").text() should include(
          messages(requiredErrorMessageKey)
        )
        verify(mockUserAnswersRepository, never).set(any())
      }
    }

    "save the platform name while preserving other session changes" in {
      val existingAnswers = UserAnswers(
        testSessionId,
        SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Assign(testOrgTelephoneNumber),
          p2pPlatform = Assign(previousP2pPlatform)
        )
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))

      val application = applicationBuilder(
        effectiveAnswers = eligibleAnswers.copy(p2pPlatform = Some(previousP2pPlatform)),
        sessionAnswers = Some(existingAnswers)
      ).build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(platformFieldName -> testP2pPlatform)
        ).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)

        val captor = ArgumentCaptor.forClass(classOf[UserAnswers])
        verify(mockUserAnswersRepository).set(captor.capture())
        captor.getValue.id      shouldBe testSessionId
        captor.getValue.updates shouldBe SessionUpdates(
          correspondenceAddress = Assign(testCorrespondenceAddress),
          organisationTelephoneNumber = Assign(testOrgTelephoneNumber),
          p2pPlatform = Assign(testP2pPlatform)
        )
      }
    }

    "redirect without saving when the platform product is not selected" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(
          application,
          FakeRequest(POST, peerToPeerPlatformEndpoint)
            .withHeaders(csrfHeaderName -> csrfHeaderValue)
            .withFormUrlEncodedBody(platformFieldName -> testP2pPlatform)
        ).value

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should endWith(changeOfCircumstancesEndpoint)
        verify(mockUserAnswersRepository, never).set(any())
      }
    }
  }
}
