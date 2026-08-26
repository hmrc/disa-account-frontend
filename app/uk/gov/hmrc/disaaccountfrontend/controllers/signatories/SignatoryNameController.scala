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

package uk.gov.hmrc.disaaccountfrontend.controllers.signatories

import play.api.Logging
import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.forms.SignatoryNameFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryNamePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.SignatoryNameView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignatoryNameController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: SignatoryNameFormProvider,
  appConfig: AppConfig,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryNameView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: Option[String]): Action[AnyContent] = (identify andThen getData) { implicit request =>
    id match {
      case None             =>
        if (signatoryCount(request) >= appConfig.maxSignatories) {
          Redirect(ChangeOfCircumstancesController.onPageLoad())
        } else {
          Redirect(routes.SignatoryNameController.onPageLoad(Some(UUID.randomUUID().toString)))
        }
      case Some(existingId) =>
        val preparedForm =
          (for {
            signatories <- request.effectiveAnswers.signatories
            signatory   <- signatories.find(_.id == existingId)
            name        <- signatory.fullName
          } yield form.fill(name)).getOrElse(form)

        Ok(view(existingId, preparedForm))
    }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(id, formWithErrors))),
        answer => {
          val isNewSignatory = !request.effectiveAnswers.signatories.exists(_.exists(_.id == id))

          if (isNewSignatory && signatoryCount(request) >= appConfig.maxSignatories) {
            Future.successful(Redirect(ChangeOfCircumstancesController.onPageLoad()))
          } else {
            val sessionUpdates = SignatoryNamePage(id).saveAnswerAndHandleDependents(request, answer)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
              .map { _ =>
                Redirect(
                  navigator.nextPage(
                    SignatoryNamePage(id),
                    sessionUpdates.getUpdatedEffectiveAnswers(request.effectiveAnswers)
                  )
                )
              }
          }
        }
      )
  }

  private def signatoryCount(request: DataRequest[_]): Int =
    request.effectiveAnswers.signatories.map(_.size).getOrElse(0)
}
