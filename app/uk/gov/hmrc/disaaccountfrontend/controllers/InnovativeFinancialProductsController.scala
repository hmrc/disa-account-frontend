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
import uk.gov.hmrc.disaaccountfrontend.models.{EffectiveAnswers, SessionUpdates, UserAnswers}
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

  def onPageLoad(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    userAnswersRepository.get(request.sessionId).map { sessionAnswers =>
      val effectiveAnswers = EffectiveAnswers.from(request.registrationDetails, sessionAnswers.map(_.updates))

      if (effectiveAnswers.hasInnovativeFinanceIsa) {
        val preparedForm = effectiveAnswers.innovativeFinancialProducts
          .fold(form)(answer => form.fill(answer.toSet))

        Ok(view(preparedForm))
      } else {
        Redirect(navigator.nextPage(InnovativeFinancialProductsPage))
      }
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    userAnswersRepository.get(request.sessionId).flatMap { sessionAnswers =>
      val effectiveAnswers = EffectiveAnswers.from(request.registrationDetails, sessionAnswers.map(_.updates))

      if (!effectiveAnswers.hasInnovativeFinanceIsa) {
        Future.successful(Redirect(navigator.nextPage(InnovativeFinancialProductsPage)))
      } else {
        form
          .bindFromRequest()
          .fold(
            formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
            answer => {
              val orderedAnswer = InnovativeFinancialProduct.values.filter(answer.contains)
              val updates       = sessionAnswers
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
}
