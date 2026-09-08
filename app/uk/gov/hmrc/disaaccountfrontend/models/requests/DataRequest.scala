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

package uk.gov.hmrc.disaaccountfrontend.models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, UserAnswers}

case class DataRequest[A](
  request: Request[A],
  zReference: String,
  credentialId: String,
  loggedInEmail: Option[String],
  sessionId: String,
  sessionAnswers: Option[UserAnswers],
  originalAnswers: Answers,
  effectiveAnswers: Answers
) extends WrappedRequest[A](request) {

  def isSignatory: Boolean =
    loggedInEmail.map(_.trim).filter(_.nonEmpty).exists { authenticatedEmail =>
      originalAnswers.signatories.exists(
        _.signatories.exists(_.email.exists(_.trim.equalsIgnoreCase(authenticatedEmail)))
      )
    }
}
