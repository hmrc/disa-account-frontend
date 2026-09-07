package uk.gov.hmrc.disaaccountfrontend.controllers.liaisonofficers

import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.disaaccountfrontend.config.AppConfig
import uk.gov.hmrc.disaaccountfrontend.controllers.actions.{DataRetrievalAction, IdentifierAction, PageGuardAction}
import uk.gov.hmrc.disaaccountfrontend.forms.generic.YesNoAnswerFormProvider
import uk.gov.hmrc.disaaccountfrontend.navigation.Navigator
import uk.gov.hmrc.disaaccountfrontend.views.html.signatories.AddedSignatoryView

import javax.inject.Inject
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

class AddedLiaisonOfficersController @Inject() (
                                                 override val messagesApi: MessagesApi,
                                                 identify: IdentifierAction,
                                                 getData: DataRetrievalAction,
                                                 guardPage: PageGuardAction,
                                                 formProvider: YesNoAnswerFormProvider,
                                                 navigator: Navigator,
                                                 appConfig: AppConfig,
                                                 val controllerComponents: MessagesControllerComponents,
                                                 view: AddedSignatoryView
                                               ) extends FrontendBaseController
  with I18nSupport {

}
