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

package uk.gov.hmrc.disaaccountfrontend.models

import play.api.i18n.Messages
import uk.gov.hmrc.disaaccountfrontend.viewmodels.govuk.checkbox.CheckboxItemViewModel
import uk.gov.hmrc.govukfrontend.views.viewmodels.checkboxes.{CheckboxItem, ExclusiveCheckbox}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text

sealed trait ChangeInformationSelection

object ChangeInformationSelection extends Enumerable.Implicits {

  case object OrganisationInformation extends WithName("organisationInformation") with ChangeInformationSelection
  case object IsaProductInformation extends WithName("isaProductInformation") with ChangeInformationSelection
  case object AuthorisedUsers extends WithName("authorisedUsers") with ChangeInformationSelection
  case object ViewAllInformation extends WithName("viewAllInformation") with ChangeInformationSelection

  val values: Seq[ChangeInformationSelection] = Seq(
    OrganisationInformation,
    IsaProductInformation,
    AuthorisedUsers,
    ViewAllInformation
  )

  val viewAllInformationFormValue = ViewAllInformation.toString

  def availableValues(isSignatory: Boolean): Seq[ChangeInformationSelection] =
    values.filterNot(selection =>
      selection == ViewAllInformation || (!isSignatory && selection == IsaProductInformation)
    )

  def validFormValues(availableSelections: Seq[ChangeInformationSelection]): Set[String] =
    availableSelections.map(_.toString).toSet + viewAllInformationFormValue

  def fromForm(
    formValues: Set[String],
    availableSelections: Seq[ChangeInformationSelection]
  ): Seq[ChangeInformationSelection] =
    if (formValues.contains(viewAllInformationFormValue)) Seq(ViewAllInformation)
    else availableSelections.filter(selection => formValues.contains(selection.toString))

  def checkboxItems(
    availableSelections: Seq[ChangeInformationSelection]
  )(implicit messages: Messages): Seq[CheckboxItem] = {
    val selectionItems = availableSelections.zipWithIndex.map { case (value, index) =>
      CheckboxItemViewModel(
        content = Text(messages(s"changeInformation.${value.toString}")),
        fieldId = "value",
        index = index,
        value = value.toString
      ).copy(name = Some("value[]"))
    }

    val divider = CheckboxItem(divider = Some(messages("changeInformation.divider")))

    val viewAll = CheckboxItemViewModel(
      content = Text(messages("changeInformation.viewAllInformation")),
      fieldId = "value",
      index = availableSelections.size,
      value = viewAllInformationFormValue
    ).copy(name = Some("value[]"), behaviour = Some(ExclusiveCheckbox))

    selectionItems ++ Seq(divider, viewAll)
  }

  implicit val enumerable: Enumerable[ChangeInformationSelection] =
    Enumerable(values.map(value => value.toString -> value): _*)
}
