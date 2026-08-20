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
import uk.gov.hmrc.disaaccountfrontend.forms.InnovativeFinancialProductsFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.IsaProduct.InnovativeFinanceIsas
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.navigation.{InnovativeFinancialProductsPage, Navigator}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.InnovativeFinancialProductsView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InnovativeFinancialProductsController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: InnovativeFinancialProductsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: InnovativeFinancialProductsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    if (request.effectiveAnswers.isaProducts.exists(_.contains(InnovativeFinanceIsas))) {
      val preparedForm = request.effectiveAnswers.innovativeFinancialProducts
        .fold(form)(answer => form.fill(answer.toSet))

      Ok(view(preparedForm))
    } else {
      Redirect(routes.ChangeOfCircumstancesController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    if (!request.effectiveAnswers.isaProducts.exists(_.contains(InnovativeFinanceIsas))) {
      Future.successful(Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
    } else {
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          answer => {
            val orderedAnswer = InnovativeFinancialProduct.values.filter(answer.contains)
            val updates       = request.sessionAnswers
              .map(_.updates)
              .getOrElse(SessionUpdates())
              .copy(innovativeFinancialProducts = Some(orderedAnswer))

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = updates))
              .map(_ => Redirect(navigator.nextPage(InnovativeFinancialProductsPage, updates)))
          }
        )
    }
  }
}
