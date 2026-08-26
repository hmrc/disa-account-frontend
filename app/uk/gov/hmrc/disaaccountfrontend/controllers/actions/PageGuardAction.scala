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
import play.api.mvc.{ActionRefiner, Call, Result}
import uk.gov.hmrc.disaaccountfrontend.controllers.routes
import uk.gov.hmrc.disaaccountfrontend.models.pages.GuardedPage
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait PageGuardAction {
  def apply(page: GuardedPage): ActionRefiner[DataRequest, DataRequest]

  def apply(page: GuardedPage, redirectLocation: => Call): ActionRefiner[DataRequest, DataRequest]
}

@Singleton
class PageGuardActionImpl @Inject() (implicit ec: ExecutionContext) extends PageGuardAction {

  override def apply(page: GuardedPage): ActionRefiner[DataRequest, DataRequest] =
    apply(page, routes.ChangeOfCircumstancesController.onPageLoad())

  override def apply(
    page: GuardedPage,
    redirectLocation: => Call
  ): ActionRefiner[DataRequest, DataRequest] = new ActionRefiner[DataRequest, DataRequest] {

    override protected val executionContext: ExecutionContext = ec

    override protected def refine[A](request: DataRequest[A]): Future[Either[Result, DataRequest[A]]] =
      if (page.canBeAccessedWith(request.effectiveAnswers)) Future.successful(Right(request))
      else Future.successful(Left(Redirect(redirectLocation)))
  }
}
