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

package uk.gov.hmrc.disaaccountfrontend.models.pages.liaisonofficers

import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, YesNoAnswer}
import uk.gov.hmrc.disaaccountfrontend.models.YesNoAnswer.{No, Yes}
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficers
import uk.gov.hmrc.disaaccountfrontend.models.pages.PageWithAnswers
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest

final case class RemoveLiaisonOfficerPage(id: String) extends PageWithAnswers[YesNoAnswer] {
  override def saveAnswerAndHandleDependents(request: DataRequest[_], newAnswer: YesNoAnswer): SessionUpdates = {
    val existingUpdates: SessionUpdates  = request.sessionAnswers.fold(SessionUpdates())(_.updates)
    val existingSection: LiaisonOfficers = request.effectiveAnswers.liaisonOfficers.getOrElse(LiaisonOfficers())

    val updatedSection = newAnswer match {
      case Yes => existingSection.updatedSectionWithLiaisonOfficerRemoved(id)

      case No => existingSection
    }

    existingUpdates.copy(liaisonOfficers = Assign(updatedSection))
  }
}
