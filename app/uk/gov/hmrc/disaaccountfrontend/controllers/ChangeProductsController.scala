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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.forms.ChangeProductsFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.ChangeProductsPage
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.ChangeProductsView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeProductsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: ChangeProductsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ChangeProductsView
)(implicit ec: ExecutionContext)
    extends PageController(navigator)
    with FrontendBaseController
    with I18nSupport {

  private val form       = formProvider()
  private val pageAction = identify andThen getData

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    if (request.isSignatory) {
      val preparedForm = request.effectiveAnswers.isaProducts.fold(form)(answer => form.fill(answer.toSet))
      Ok(view(preparedForm))
    } else {
      Redirect(routes.ChangeOfCircumstancesController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    if (request.isSignatory) {
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          answer => {
            val sessionUpdates = getSessionUpdates(ChangeProductsPage, answer)
            val updatedAnswers = sessionUpdates.getUpdatedEffectiveAnswers(request.originalAnswers)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
              .map(_ => Redirect(navigator.nextPageFromChangeProducts(updatedAnswers)))
          }
        )
    } else {
      Future.successful(Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
    }
  }
}
