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

package uk.gov.hmrc.disaaccountfrontend.models

import play.api.libs.json.*

sealed trait AnswerUpdate[+A] {
  def getEffectiveAnswer[B >: A](answer: Option[B]): Option[B] = this match {
    case AnswerUpdate.Unchanged     => answer
    case AnswerUpdate.Assign(value) => Some(value)
    case AnswerUpdate.Clear         => None
  }
}

object AnswerUpdate {
  case object Unchanged extends AnswerUpdate[Nothing]
  final case class Assign[A](value: A) extends AnswerUpdate[A]
  case object Clear extends AnswerUpdate[Nothing]

  private val StateField     = "state"
  private val ValueField     = "value"
  private val UnchangedState = "unchanged"
  private val SetState       = "set"
  private val ClearState     = "clear"

  implicit def format[A](implicit reads: Reads[A], valueWrites: Writes[A]): Format[AnswerUpdate[A]] =
    new Format[AnswerUpdate[A]] {
      override def reads(json: JsValue): JsResult[AnswerUpdate[A]] = json match {
        case JsNull                                                     => JsSuccess(Unchanged)
        case value: JsObject if (value \ StateField).toOption.isDefined =>
          (value \ StateField).validate[String].flatMap {
            case UnchangedState => JsSuccess(Unchanged)
            case SetState       => (value \ ValueField).validate[A].map(Assign.apply)
            case ClearState     => JsSuccess(Clear)
            case state          => JsError(JsPath \ StateField, s"Unknown answer update state: $state")
          }
        case legacyValue                                                => legacyValue.validate[A].map(Assign.apply)
      }

      override def writes(update: AnswerUpdate[A]): JsValue = update match {
        case Unchanged     => Json.obj(StateField -> UnchangedState)
        case Assign(value) => Json.obj(StateField -> SetState, ValueField -> valueWrites.writes(value))
        case Clear         => Json.obj(StateField -> ClearState)
      }
    }
}
