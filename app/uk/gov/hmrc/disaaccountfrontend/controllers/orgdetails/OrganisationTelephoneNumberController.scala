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

package uk.gov.hmrc.disaaccountfrontend.controllers.orgdetails

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.TelephoneNumberFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.navigation.{Navigator, OrganisationTelephoneNumberPage}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.OrganisationTelephoneNumberView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OrganisationTelephoneNumberController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: TelephoneNumberFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: OrganisationTelephoneNumberView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider("organisationTelephoneNumber")

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    val preparedForm = request.effectiveAnswers.organisationTelephoneNumber.fold(form)(form.fill)
    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = identify.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        answer =>
          userAnswersRepository.get(request.sessionId).flatMap { existing =>
            val updates =
              existing.map(_.updates).getOrElse(SessionUpdates()).copy(organisationTelephoneNumber = Some(answer))
            userAnswersRepository.set(UserAnswers(id = request.sessionId, updates = updates)).map { _ =>
              Redirect(navigator.nextPage(OrganisationTelephoneNumberPage))
            }
          }
      )
  }
}
