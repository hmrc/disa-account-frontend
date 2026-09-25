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

package uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.changeOfCircumstances

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.Answers
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryListRow

final case class FcaArticlesChanges(added: Seq[FcaArticles], removed: Seq[FcaArticles]) {

  val hasChanges: Boolean = added.nonEmpty || removed.nonEmpty

  def addedNames(implicit messages: Messages): Seq[String]   = added.map(FcaArticlesSummary.articleName)
  def removedNames(implicit messages: Messages): Seq[String] = removed.map(FcaArticlesSummary.articleName)

  def rows(implicit messages: Messages): Seq[SummaryListRow] =
    Seq(
      Option.when(added.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.articlesAdded", addedNames)
      ),
      Option.when(removed.nonEmpty)(
        ChangesSummaryRow("changeOfCircumstances.checkYourChanges.articlesRemoved", removedNames)
      )
    ).flatten
}

object FcaArticlesChanges {

  def apply(original: Answers, effective: Answers): FcaArticlesChanges = {
    val originalArticles  = original.fcaArticles.getOrElse(Seq.empty).toSet
    val effectiveArticles = effective.fcaArticles.getOrElse(Seq.empty).toSet

    FcaArticlesChanges(
      added = FcaArticles.values.filter(article => effectiveArticles(article) && !originalArticles(article)),
      removed = FcaArticles.values.filter(article => originalArticles(article) && !effectiveArticles(article))
    )
  }
}
