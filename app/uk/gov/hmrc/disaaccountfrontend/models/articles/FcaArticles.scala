package uk.gov.hmrc.disaaccountfrontend.models.articles

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.{Enumerable, WithName}
import uk.gov.hmrc.disaaccountfrontend.viewmodels.govuk.checkbox.*
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.CheckboxItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait FcaArticles

object FcaArticles extends Enumerable.Implicits {

  case object FcaArticle14
    extends WithName("FcaArticle14")
      with FcaArticles

  case object FcaArticle21
    extends WithName("FcaArticle21")
      with FcaArticles

  case object FcaArticle25
    extends WithName("FcaArticle25")
      with FcaArticles

  case object FcaArticle36H
    extends WithName("FcaArticle36H")
      with FcaArticles

  case object FcaArticle37
    extends WithName("FcaArticle37")
      with FcaArticles

  case object FcaArticle39G
    extends WithName("FcaArticle39G")
      with FcaArticles

  case object FcaArticle40
    extends WithName("FcaArticle40")
      with FcaArticles

  case object FcaArticle45
    extends WithName("FcaArticle45")
      with FcaArticles

  case object FcaArticle51ZA
    extends WithName("FcaArticle51ZA")
      with FcaArticles

  case object FcaArticle51ZC
    extends WithName("FcaArticle51ZC")
      with FcaArticles

  case object FcaArticle51ZE
    extends WithName("FcaArticle51ZE")
      with FcaArticles

  case object FcaArticle53
    extends WithName("FcaArticle53")
      with FcaArticles

  case object FcaArticle64
    extends WithName("FcaArticle64")
      with FcaArticles

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

  implicit val enumerable: Enumerable[InnovativeFinancialProduct] =
    Enumerable(values.map(value => value.toString -> value): _*)
}
