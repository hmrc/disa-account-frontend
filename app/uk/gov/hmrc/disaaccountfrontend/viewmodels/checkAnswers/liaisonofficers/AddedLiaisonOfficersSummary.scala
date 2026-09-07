package uk.gov.hmrc.disaaccountfrontend.viewmodels.checkAnswers.liaisonofficers

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers.LiaisonOfficer
import uk.gov.hmrc.govukfrontend.views.Aliases.{ActionItem, Actions, Key, SummaryList, SummaryListRow, Text, Value}

case class AddedLiaisonOfficersSummary(liaisonOfficers: Seq[LiaisonOfficer], maxOfficers: Int) {

  private val completeOfficers = liaisonOfficers.filter(_.isComplete)

  val count:Int = completeOfficers.size
  val canAddMore: Boolean = count < maxOfficers

  def title(implicit messages: Messages): String =
    if (count == 1) messages("addedLiaisonOfficer.title")
    else messages("addedLiaisonOfficer.title.plural", count)

  def guidance(implicit messages: Messages): String =
    if (canAddMore) messages("addedLiaisonOfficer.guidance", maxOfficers)
    else messages("addedLiaisonOfficer.guidance.max", maxOfficers)

  def list(implicit messages: Messages): SummaryList =
    SummaryList(rows = completeOfficers.flatMap(row))

  private def row(officer: LiaisonOfficer)(implicit messages: Messages): Option[SummaryListRow] = {
    officer.fullName.map{name =>
      SummaryListRow(
        key = Key(Text(name), classes = "govuk-!-font-weight-regular"),
        value = Value(Text(""), classes = "govuk-!-width-one-quarter"),
        actions = Some(Actions(
          items = Seq(
            ActionItem(
              href = ???,
              content = Text(messages("site.change")),
              visuallyHiddenText = Some(messages("addedLiaisonOfficer.summary.action.hidden", name)),
              ),
            ActionItem(
              href = ???,
              content = Text(messages("site.remove")),
              visuallyHiddenText = Some(messages("addedLiaisonOfficer.summary.action.hidden", name))))
          )
        )
      )
    }
  }
}
