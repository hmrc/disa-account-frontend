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
import uk.gov.hmrc.disaaccountfrontend.controllers.routes
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

// Guards pages that only a signatory should be able to reach (signatory management, ISA product
// changes). Unlike PageGuardAction - which decides reachability from the answers already on the
// account - this decides on who is asking, so it belongs in its own action rather than as another
// GuardedPage.
trait RequireSignatoryAction extends ActionFilter[DataRequest]

@Singleton
class RequireSignatoryActionImpl @Inject() (implicit val executionContext: ExecutionContext)
    extends RequireSignatoryAction {

  override protected def filter[A](request: DataRequest[A]): Future[Option[Result]] =
    Future.successful(
      Option.unless(request.isSignatory)(Redirect(routes.ChangeOfCircumstancesController.onPageLoad()))
    )
}
