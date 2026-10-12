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

package uk.gov.hmrc.disaaccountfrontend.controllers.actions

import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait RemoveIncompleteContactsAction extends ActionRefiner[DataRequest, DataRequest]

@Singleton
class RemoveIncompleteContactsActionImpl @Inject() (
  userAnswersRepository: UserAnswersRepository
)(implicit val executionContext: ExecutionContext)
    extends RemoveIncompleteContactsAction {

  override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] = {
    val cleanedSignatories     = request.effectiveAnswers.signatories.map(section =>
      section.copy(signatories = section.signatories.filter(_.isComplete))
    )
    val cleanedLiaisonOfficers = request.effectiveAnswers.liaisonOfficers.map(section =>
      section.copy(liaisonOfficers = section.liaisonOfficers.filter(_.isComplete))
    )
    val signatoriesChanged     = cleanedSignatories != request.effectiveAnswers.signatories
    val liaisonOfficersChanged = cleanedLiaisonOfficers != request.effectiveAnswers.liaisonOfficers

    if (!signatoriesChanged && !liaisonOfficersChanged) {
      Future.successful(Right(request))
    } else {
      val existingUpdates = request.sessionAnswers.fold(SessionUpdates())(_.updates)
      val cleanedUpdates  = existingUpdates.copy(
        signatories = if (signatoriesChanged) Assign(cleanedSignatories.get) else existingUpdates.signatories,
        liaisonOfficers =
          if (liaisonOfficersChanged) Assign(cleanedLiaisonOfficers.get) else existingUpdates.liaisonOfficers
      )
      val cleanedAnswers  = UserAnswers(request.sessionId, cleanedUpdates)

      userAnswersRepository.set(cleanedAnswers).map { _ =>
        Right(
          request.copy(
            sessionAnswers = Some(cleanedAnswers),
            effectiveAnswers = request.effectiveAnswers.copy(
              signatories = cleanedSignatories,
              liaisonOfficers = cleanedLiaisonOfficers
            )
          )
        )
      }
    }
  }
}
