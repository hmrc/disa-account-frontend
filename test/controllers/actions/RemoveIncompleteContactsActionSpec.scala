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

package controllers.actions

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.RemoveIncompleteContactsActionImpl
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.{LiaisonOfficer, LiaisonOfficerCommunication, LiaisonOfficers}
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.signatories.{Signatories, Signatory}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates, UserAnswers}
import utils.BaseUnitSpec

import scala.concurrent.Future

class RemoveIncompleteContactsActionSpec extends BaseUnitSpec {

  private val completedSignatory  = Signatory("complete-signatory", Some("Jane Doe"), Some("Director"))
  private val incompleteSignatory = Signatory("incomplete-signatory", Some("John Smith"))
  private val completedOfficer    = LiaisonOfficer(
    "complete-officer",
    Some("Sam Jones"),
    Some("0123456789"),
    Set(LiaisonOfficerCommunication.values.head),
    Some("sam@example.com")
  )
  private val incompleteOfficer   = LiaisonOfficer("incomplete-officer", Some("Alex Brown"))

  "RemoveIncompleteContactsAction" should {

    "remove all incomplete contacts while preserving completed contacts and other updates" in {
      val effectiveAnswers = Answers(
        signatories = Some(Signatories(Seq(completedSignatory, incompleteSignatory))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(incompleteOfficer, completedOfficer)))
      )
      val existingUpdates  = SessionUpdates(
        tradingName = Assign("Updated trading name"),
        signatories = Assign(effectiveAnswers.signatories.value),
        liaisonOfficers = Assign(effectiveAnswers.liaisonOfficers.value)
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val action           = new RemoveIncompleteContactsActionImpl(mockUserAnswersRepository)
      val result           = action.invokeBlock(
        dataRequest(effectiveAnswers, Some(UserAnswers(testSessionId, existingUpdates))),
        request => {
          request.effectiveAnswers.signatories     shouldBe Some(Signatories(Seq(completedSignatory)))
          request.effectiveAnswers.liaisonOfficers shouldBe Some(LiaisonOfficers(Seq(completedOfficer)))
          Future.successful(Ok)
        }
      )
      val captor           = ArgumentCaptor.forClass(classOf[UserAnswers])

      status(result)                          shouldBe 200
      verify(mockUserAnswersRepository).set(captor.capture())
      captor.getValue.updates.tradingName     shouldBe Assign("Updated trading name")
      captor.getValue.updates.signatories     shouldBe Assign(Signatories(Seq(completedSignatory)))
      captor.getValue.updates.liaisonOfficers shouldBe Assign(LiaisonOfficers(Seq(completedOfficer)))
    }

    "store an explicit empty collection when every contact is incomplete" in {
      val effectiveAnswers = Answers(
        signatories = Some(Signatories(Seq(incompleteSignatory))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(incompleteOfficer)))
      )
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.successful(true))
      val action           = new RemoveIncompleteContactsActionImpl(mockUserAnswersRepository)
      val result           = action.invokeBlock(dataRequest(effectiveAnswers), _ => Future.successful(Ok))
      val captor           = ArgumentCaptor.forClass(classOf[UserAnswers])

      status(result)                          shouldBe 200
      verify(mockUserAnswersRepository).set(captor.capture())
      captor.getValue.updates.signatories     shouldBe Assign(Signatories())
      captor.getValue.updates.liaisonOfficers shouldBe Assign(LiaisonOfficers())
    }

    "not write to the repository when all contacts are complete" in {
      val effectiveAnswers = Answers(
        signatories = Some(Signatories(Seq(completedSignatory))),
        liaisonOfficers = Some(LiaisonOfficers(Seq(completedOfficer)))
      )
      val action           = new RemoveIncompleteContactsActionImpl(mockUserAnswersRepository)
      val result           = action.invokeBlock(dataRequest(effectiveAnswers), _ => Future.successful(Ok))

      status(result) shouldBe 200
      verify(mockUserAnswersRepository, never()).set(any())
    }

    "propagate repository failures" in {
      val exception        = new RuntimeException("boom")
      val effectiveAnswers = Answers(signatories = Some(Signatories(Seq(incompleteSignatory))))
      when(mockUserAnswersRepository.set(any())).thenReturn(Future.failed(exception))
      val action           = new RemoveIncompleteContactsActionImpl(mockUserAnswersRepository)
      val result           = action.invokeBlock(dataRequest(effectiveAnswers), _ => Future.successful(Ok))

      result.failed.futureValue shouldBe exception
    }
  }

  private def dataRequest(
    answers: Answers,
    sessionAnswers: Option[UserAnswers] = None
  ): DataRequest[play.api.mvc.AnyContentAsEmpty.type] =
    DataRequest(
      FakeRequest(),
      testZref,
      testCredentialId,
      None,
      testSessionId,
      sessionAnswers,
      originalAnswers = Answers(),
      effectiveAnswers = answers
    )
}
