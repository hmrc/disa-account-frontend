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
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes.ChangeOfCircumstancesController
import uk.gov.hmrc.disaaccountfrontend.forms.SignatoryJobTitleFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers
import uk.gov.hmrc.disaaccountfrontend.models.pages.SignatoryJobTitlePage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.repositories.UserAnswersRepository
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.SignatoryJobTitleView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SignatoryJobTitleController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  getData: DataRetrievalAction,
  userAnswersRepository: UserAnswersRepository,
  navigator: Navigator,
  formProvider: SignatoryJobTitleFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: SignatoryJobTitleView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  val form: Form[String] = formProvider()

  def onPageLoad(id: String): Action[AnyContent] = (identify andThen getData) { implicit request =>
    signatoryName(id, request).fold(Redirect(ChangeOfCircumstancesController.onPageLoad())) { name =>
      val preparedForm =
        (for {
          signatories <- request.effectiveAnswers.signatories
          signatory   <- signatories.find(_.id == id)
          jobTitle    <- signatory.jobTitle
        } yield form.fill(jobTitle)).getOrElse(form)

      Ok(view(id, name, preparedForm))
    }
  }

  def onSubmit(id: String): Action[AnyContent] = (identify andThen getData).async { implicit request =>
    signatoryName(id, request).fold(Future.successful(Redirect(ChangeOfCircumstancesController.onPageLoad()))) { name =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(id, name, formWithErrors))),
          answer => {
            val sessionUpdates = SignatoryJobTitlePage(id).saveAnswerAndHandleDependents(request, answer)

            userAnswersRepository
              .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
              .map { _ =>
                Redirect(
                  navigator.nextPage(
                    SignatoryJobTitlePage(id),
                    sessionUpdates.getUpdatedEffectiveAnswers(request.effectiveAnswers)
                  )
                )
              }
          }
        )
    }
  }

  private def signatoryName(id: String, request: DataRequest[_]): Option[String] =
    for {
      signatories <- request.effectiveAnswers.signatories
      signatory   <- signatories.find(_.id == id)
      name        <- signatory.fullName
    } yield name
}
