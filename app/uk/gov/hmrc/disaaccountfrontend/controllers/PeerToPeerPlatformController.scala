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
import uk.gov.hmrc.disaaccountfrontend.forms.PeerToPeerPlatformFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.AnswerUpdate.Assign
import uk.gov.hmrc.disaaccountfrontend.models.isaproducts.InnovativeFinancialProduct.PeertopeerLoansUsingAPlatformWith36hPermissions
import uk.gov.hmrc.disaaccountfrontend.models.{SessionUpdates, UserAnswers}
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.{Navigator, PeerToPeerPlatformPage}
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.PeerToPeerPlatformView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class PeerToPeerPlatformController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: PeerToPeerPlatformFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: PeerToPeerPlatformView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val form = formProvider()

  private def canAccess(request: DataRequest[_]): Boolean =
    request.effectiveAnswers.innovativeFinancialProducts.exists(
      _.contains(PeertopeerLoansUsingAPlatformWith36hPermissions)
    )

  def onPageLoad(): Action[AnyContent] = (identify andThen getData) { implicit request =>
    if (canAccess(request)) {
      val preparedForm = request.effectiveAnswers.p2pPlatform.fold(form)(form.fill)
      Ok(view(preparedForm))
    } else {
      Redirect(routes.ChangeOfCircumstancesController.onPageLoad())
    }
  }

  def onSubmit(): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    if (!canAccess(request)) {
      Future.successful(Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
    } else {
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
          answer => {
            val existingUpdates   = request.sessionAnswers.map(_.updates).getOrElse(SessionUpdates())
            val updates           = existingUpdates.copy(p2pPlatform = Assign(answer))
            val navigationAnswers = updates.getEffectiveAnswers(request.effectiveAnswers)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = updates))
              .map(_ => Redirect(navigator.nextPage(PeerToPeerPlatformPage, navigationAnswers)))
          }
        )
    }
  }
}
