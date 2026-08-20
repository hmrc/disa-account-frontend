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

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.BaseUnitSpec

import java.net.{URI, URLDecoder}
import java.nio.charset.StandardCharsets
import scala.concurrent.Future

class AuthControllerSpec extends BaseUnitSpec {

  "AuthController.signOut" should {

    "clear the saved session answers and redirect to bas-gateway sign-out with a continue to the signed-out page" in {
      when(mockUserAnswersRepository.clear(eqTo(testSessionId))).thenReturn(Future.successful(true))

      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, signOutEndpoint)).value

        val redirectUrl = redirectLocation(result).value

        status(result)                                                                 shouldBe SEE_OTHER
        redirectUrl                                                                      should startWith(basGatewaySignOutEndpoint)
        URLDecoder.decode(URI.create(redirectUrl).getRawQuery, StandardCharsets.UTF_8) shouldBe
          s"continue=$signedOutEndpoint"
        verify(mockUserAnswersRepository).clear(testSessionId)
      }
    }

    "still redirect to sign-out when clearing the saved session answers fails" in {
      when(mockUserAnswersRepository.clear(eqTo(testSessionId)))
        .thenReturn(Future.failed(new RuntimeException("Boom")))

      val application = applicationBuilder().build()

      running(application) {
        val result = route(application, FakeRequest(GET, signOutEndpoint)).value

        status(result)             shouldBe SEE_OTHER
        redirectLocation(result).get should startWith(basGatewaySignOutEndpoint)
      }
    }
  }
}
