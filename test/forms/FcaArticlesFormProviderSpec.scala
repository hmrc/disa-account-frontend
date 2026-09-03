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

package forms

//import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.data.Form
import uk.gov.hmrc.disaaccountfrontend.forms.FcaArticlesFormProvider
import uk.gov.hmrc.disaaccountfrontend.models.articles.FcaArticles
import utils.BaseUnitSpec

class FcaArticlesFormProviderSpec extends BaseUnitSpec {
  val form: Form[Set[FcaArticles]] = new FcaArticlesFormProvider()()

  "FcaArticlesFormProvider" should {

    "bind every supported product" in
      FcaArticles.values.zipWithIndex.foreach { case (product, index) =>
        val result = form.bind(Map(s"value[$index]" -> product.toString))

        result.errors shouldBe empty
        result.value  shouldBe Some(Set(product))
      }

    "bind multiple products" in {
      val selected = FcaArticles.values.take(2)
      val result   = form.bind(selected.zipWithIndex.map { case (product, index) =>
        s"value[$index]" -> product.toString
      }.toMap)

      result.errors shouldBe empty
      result.value  shouldBe Some(selected.toSet)
    }

    "return the required error when no product is selected" in {
      val result = form.bind(Map.empty[String, String])

      result.errors.map(_.message) should contain("FcaArticles.error.required")
    }

    "reject an unsupported product" in {
      val result = form.bind(Map("value[0]" -> "unsupported"))

      result.errors.map(_.message) should contain("error.invalid")
    }

    "fill previously selected products" in {
      val selected = FcaArticles.values.takeRight(2).toSet
      val filled   = form.fill(selected)

      filled.value             shouldBe Some(selected)
      filled.data.values.toSet shouldBe selected.map(_.toString)
    }
  }
}
