package uk.gov.hmrc.disaaccountfrontend.controllers

import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.disaaccountfrontend.models.UserAnswers

import scala.concurrent.Future

class FCAArticlesOrganisationController @Inject()(
                                                   override val messagesApi: MessagesApi,
                                                   identify: IdentifierAction,
                                                   getData: DataRetrievalAction,
                                                   guardPage: PageGuardAction,
                                                   userAnswersRepository: UserAnswersRepository,
                                                   navigator: Navigator,
                                                   formProvider: PeerToPeerPlatformFormProvider,
                                                   val controllerComponents: MessagesControllerComponents,
                                                   view: PeerToPeerPlatformView
                                                 )(implicit ec: ExecutionContext)
  extends PageController(PeerToPeerPlatformPage, navigator)
    with FrontendBaseController
    with I18nSupport {

  private val form = formProvider()
  private val pageAction = identify andThen getData andThen guardPage(page)

  def onPageLoad(): Action[AnyContent] = pageAction { implicit request =>
    val preparedForm = request.effectiveAnswers.p2pPlatform.fold(form)(form.fill)
    Ok(view(preparedForm))
  }

  def onSubmit(): Action[AnyContent] = pageAction.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        answer => {
          val sessionUpdates = getSessionUpdates(answer)

          userAnswersRepository
            .set(UserAnswers(id = request.sessionId, updates = sessionUpdates))
            .map(_ => Redirect(nextPage(sessionUpdates)))
        }
      )
  }
}
