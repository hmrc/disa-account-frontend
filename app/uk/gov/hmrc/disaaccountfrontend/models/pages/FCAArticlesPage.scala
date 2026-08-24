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

package uk.gov.hmrc.disaaccountfrontend.models.pages

import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.{Assign, Clear}
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles.FcaArticle14
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates}
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest

case object FCAArticlesPage extends GuardedPage with PageWithAnswers[Set[FcaArticles]] {

  def canBeAccessedWith(answers: Answers): Boolean = answers.fcaArticles.exists(
    _.contains(FcaArticles)
  )

  def saveAnswerAndHandleDependents(request: DataRequest[_], newAnswer: Set[FcaArticles]): SessionUpdates = {
    val existingUpdates = request.sessionAnswers.fold(SessionUpdates())(_.updates)
    existingUpdates.copy(fcaArticles = Assign(newAnswer.toSeq))

    val platformProductWasRemoved =
      request.effectiveAnswers.fcaArticles.exists(
        _.contains(FcaArticles.FcaArticle14)
      ) && !newAnswer.contains(FcaArticle14)
      
    val hasPlatformAnswers =
      request.effectiveAnswers.fcaArticles.isDefined

    if (platformProductWasRemoved && hasPlatformAnswers) {
      existingUpdates.copy(fcaArticles = Assign(newAnswer.toSeq))
      } 
    else {
      existingUpdates.copy(fcaArticles = Assign(newAnswer.toSeq))
    }
      
  }
}
