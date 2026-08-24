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

package uk.gov.hmrc.disaaccountfrontend.models.articles

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.{Enumerable, WithName}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.govuk.checkbox.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait FcaArticles

object FcaArticles extends Enumerable.Implicits {

  case object FcaArticle14 extends WithName("article14") with FcaArticles

  case object FcaArticle21 extends WithName("article21") with FcaArticles

  case object FcaArticle25 extends WithName("article25") with FcaArticles

  case object FcaArticle36H extends WithName("article36H") with FcaArticles

  case object FcaArticle37 extends WithName("article37") with FcaArticles

  case object FcaArticle39G extends WithName("article39G") with FcaArticles

  case object FcaArticle40 extends WithName("article40") with FcaArticles

  case object FcaArticle45 extends WithName("article45") with FcaArticles

  case object FcaArticle51ZA extends WithName("article51ZA") with FcaArticles

  case object FcaArticle51ZC extends WithName("article51ZC") with FcaArticles

  case object FcaArticle51ZE extends WithName("article51ZE") with FcaArticles

  case object FcaArticle53 extends WithName("article53") with FcaArticles

  case object FcaArticle64 extends WithName("article64") with FcaArticles

  val values: Seq[FcaArticles] = Seq(
    FcaArticle14,
    FcaArticle21,
    FcaArticle25,
    FcaArticle36H,
    FcaArticle37,
    FcaArticle39G,
    FcaArticle40,
    FcaArticle45,
    FcaArticle51ZA,
    FcaArticle51ZC,
    FcaArticle51ZE,
    FcaArticle53,
    FcaArticle64
  )

  def checkboxItems(implicit messages: Messages): Seq[CheckboxItem] =
    values.zipWithIndex.map { case (value, index) =>
      CheckboxItemViewModel(
        content = Text(messages(s"FCAArticlesOrganisation.${value.toString}")),
        fieldId = "value",
        index = index,
        value = value.toString
      )
    }

  implicit val enumerable: Enumerable[FcaArticles] =
    Enumerable(values.map(value => value.toString -> value): _*)
}
