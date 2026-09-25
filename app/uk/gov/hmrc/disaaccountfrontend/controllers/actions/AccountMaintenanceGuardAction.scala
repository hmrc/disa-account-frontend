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

package uk.gov.hmrc.disaaccountfrontend.controllers.actions

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.disaaccountfrontend.connectors.RegistrationConnector
import uk.gov.hmrc.disaaccountfrontend.controllers.routes
import uk.gov.hmrc.disaaccountfrontend.models.registration.RegistrationDetails
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AccountMaintenanceGuardAction extends ActionFilter[DataRequest]

@Singleton
class AccountMaintenanceGuardActionImpl @Inject() (
  registrationConnector: RegistrationConnector
)(implicit val executionContext: ExecutionContext)
    extends AccountMaintenanceGuardAction {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] = {
    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    registrationConnector.getRegistrationDetails(request.zReference).map { registration =>
      Option.when(isaProductsChangeUnderReview(registration))(Redirect(routes.ManageIsasController.onPageLoad()))
    }
  }

  private def isaProductsChangeUnderReview(registration: Option[RegistrationDetails]): Boolean =
    registration.map(_.isaProductsChangeUnderReview).contains(true)
}
