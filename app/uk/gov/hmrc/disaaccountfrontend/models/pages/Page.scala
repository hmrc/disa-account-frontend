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

package uk.gov.hmrc.disaaccountfrontend.models.pages

import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest
import uk.gov.hmrc.disaaccountfrontend.models.{Answers, SessionUpdates}

sealed trait Page

trait GuardedPage extends Page {
  def canBeAccessedWith(answers: Answers): Boolean
}

trait PageWithAnswers[A] extends Page {
  def saveAnswerAndHandleDependents(request: DataRequest[_], newAnswer: A): SessionUpdates
}
