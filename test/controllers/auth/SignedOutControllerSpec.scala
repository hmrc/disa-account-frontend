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

package controllers.auth

import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.BaseUnitSpec

class SignedOutControllerSpec extends BaseUnitSpec {

  // prod.routes mounts app.routes under this prefix, which the per-controller reverse router doesn't know about.
  val signedOutUrl: String = "/obligations/account/isa/signed-out"

  "SignedOutController.onPageLoad" should {

    "return 200 OK and render the signed-out view" in {
      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, signedOutUrl)).value

        status(result)        shouldBe OK
        contentAsString(result) should include("For your security, we signed you out")
        contentAsString(result) should include("We did not save your answers.")
        contentAsString(result) should include("Sign in")
      }
    }
  }
}
