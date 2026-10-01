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

package uk.gov.hmrc.disaaccountfrontend.controllers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{AccountMaintenanceGuardAction, DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.disaaccountfrontend.models.registration.UpdateRegistrationDetailsRequest
import uk.gov.hmrc.disaaccountfrontend.views.html.DeclarationForChangesView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class DeclarationForChangesController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  accountMaintenanceGuard: AccountMaintenanceGuardAction,
  registrationConnector: RegistrationConnector,
  val controllerComponents: MessagesControllerComponents,
  view: DeclarationForChangesView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val pageAction = identify andThen getData andThen accountMaintenanceGuard

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    Ok(view(request.isaProductsUpdated))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    registrationConnector
      .updateRegistrationDetails(request.zReference, UpdateRegistrationDetailsRequest(request.effectiveAnswers))
      .map(_ => Redirect(routes.ChangesCompletedController.onPageLoad()))
  }
}
