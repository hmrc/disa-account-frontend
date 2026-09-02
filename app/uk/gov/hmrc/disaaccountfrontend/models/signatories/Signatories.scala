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

package uk.gov.hmrc.disaaccountfrontend.models.signatories

import play.api.libs.json.{Json, OFormat}

case class Signatories(signatories: Seq[Signatory] = Seq.empty[Signatory]) {
  def upsertName(id: String, fullName: String): Signatories =
    if (signatories.exists(_.id == id)) {
      copy(
        signatories = signatories.map {
          case officer if officer.id == id => officer.copy(fullName = Some(fullName))
          case officer                     => officer
        }
      )
    } else {
      copy(signatories = signatories :+ Signatory(id = id, fullName = Some(fullName)))
    }
}

object Signatories {
  val sectionName: String                   = "signatories"
  implicit val format: OFormat[Signatories] = Json.format[Signatories]
}
