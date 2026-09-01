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

package uk.gov.hmrc.disaaccountfrontend.models.liaisonofficers

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.disaaccountfrontend.models.requests.DataRequest

case class LiaisonOfficers(liaisonOfficers: Seq[LiaisonOfficer] = Seq.empty) {

  def upsertName(id: String, fullName: String): LiaisonOfficers =
    if (liaisonOfficers.exists(_.id == id)) {
      copy(
        liaisonOfficers = liaisonOfficers.map {
          case officer if officer.id == id => officer.copy(fullName = Some(fullName))
          case officer                     => officer
        }
      )
    } else {
      copy(liaisonOfficers = liaisonOfficers :+ LiaisonOfficer(id = id, fullName = Some(fullName)))
    }

  def updateEmail(id: String, email: String): LiaisonOfficers =
    copy(
      liaisonOfficers = liaisonOfficers.map {
        case officer if officer.id == id => officer.copy(email = Some(email))
        case officer                     => officer
      }
    )
}

object LiaisonOfficers {
  implicit val format: OFormat[LiaisonOfficers] = Json.format[LiaisonOfficers]

  def findLiaisonOfficer(id: String)(implicit request: DataRequest[_]): Option[LiaisonOfficer] =
    request.effectiveAnswers.liaisonOfficers.flatMap(_.liaisonOfficers.find(_.id == id))
}
