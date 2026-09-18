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

package uk.gov.hmrc.disaaccountfrontend

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module as AppModule}
import uk.gov.hmrc.disaaccountfrontend.config.{InternalAuthTokenInitialiser, InternalAuthTokenInitialiserImpl, NoOpInternalAuthTokenInitialiser}
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{AuthenticatedIdentifierAction, DataRetrievalAction, DataRetrievalActionImpl, IdentifierAction, PageGuardAction, PageGuardActionImpl}

import java.time.Clock

class Module extends AppModule {

  override def bindings(
    environment: Environment,
    configuration: Configuration
  ): Seq[Binding[_]] = {

    val authTokenInitialiserBindings: Seq[Binding[_]] =
      if (configuration.get[Boolean]("create-internal-auth-token-on-start")) {
        Seq(bind[InternalAuthTokenInitialiser].to[InternalAuthTokenInitialiserImpl])
      } else {
        Seq(bind[InternalAuthTokenInitialiser].to[NoOpInternalAuthTokenInitialiser])
      }

    Seq(
      bind[Clock].toInstance(Clock.systemDefaultZone),
      bind[IdentifierAction].to[AuthenticatedIdentifierAction],
      bind[DataRetrievalAction].to[DataRetrievalActionImpl],
      bind[PageGuardAction].to[PageGuardActionImpl],
      bind[AppInitialiser].toSelf.eagerly()
    ) ++ authTokenInitialiserBindings
  }
}
