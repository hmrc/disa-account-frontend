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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.InnovativeFinancialProductsFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.InnovativeFinancialProductsPage
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.InnovativeFinancialProductsView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InnovativeFinancialProductsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  guardPage: PageGuardAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: InnovativeFinancialProductsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InnovativeFinancialProductsView
)(implicit ec: ExecutionContext)
    extends PageController(InnovativeFinancialProductsPage, navigator)
    with FrontendBaseController
    with I18nSupport {

  private val form       = formProvider()
  private val pageAction = identify andThen getData andThen guardPage(page)

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    val preparedForm = request.effectiveAnswers.innovativeFinancialProducts
      .fold(form)(answer => form.fill(answer.toSet))

    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        answer => {
          val sessionUpdates = getSessionUpdates(answer)

          userAnswersRepository
            .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
            .map(_ => Redirect(nextPage(sessionUpdates)))
        }
      )
  }
}
