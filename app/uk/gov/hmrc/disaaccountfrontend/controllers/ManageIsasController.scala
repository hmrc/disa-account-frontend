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
import uk.gov.hmrc.disaaccountfrontend.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.disaaccountfrontend.connectors.{RegistrationConnector, ReportingWindowConnector}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.IdentifierAction
import uk.gov.hmrc.disaaccountfrontend.services.ReportingPeriodService
import uk.gov.hmrc.disaaccountfrontend.views.html.ManageIsasView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ManageIsasController @Inject() (
  override val messagesApi: MessagesApi,
  identify: IdentifierAction,
  registrationConnector: RegistrationConnector,
  reportingWindowConnector: ReportingWindowConnector,
  reportingPeriodService: ReportingPeriodService,
  appConfig: AppConfig,
  errorHandler: ErrorHandler,
  val controllerComponents: MessagesControllerComponents,
  view: ManageIsasView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(): Action[AnyContent] = identify.async { implicit request =>
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val registrationDetails   = registrationConnector.getRegistrationDetails(request.zReference)
    val reportingWindowStatus = reportingWindowConnector.isReportingWindowOpen(request.zReference)

    registrationDetails.zip(reportingWindowStatus).flatMap { case (registration, reportingWindowOpen) =>
      registration.flatMap(_.companyName) match {
        case None              => errorHandler.internalServerError
        case Some(companyName) =>
          Future.successful(
            Ok(
              view(
                companyName = companyName,
                reportingWindowOpen = reportingWindowOpen,
                reportingWindowMonth = reportingPeriodService.reportingWindowMonth,
                reportingPeriodMonth = reportingPeriodService.reportingPeriodMonth,
                closingDateFormatted = reportingPeriodService.closingDateFormatted,
                daysRemaining = reportingPeriodService.daysRemaining,
                submitReportUrl = appConfig.monthlyReportSubmissionUrl,
                isaProductsChangeUnderReview = registration.exists(_.isaProductsChangeUnderReview)
              )
            )
          )
      }
    }
  }
}
