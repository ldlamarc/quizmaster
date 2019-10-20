package app.flux.react.app.quiz

import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Size
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.flux.react.uielements.PageHeader
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.router.RouterContext
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

final class MasterView(
    implicit pageHeader: PageHeader,
    dispatcher: Dispatcher,
    quizConfig: QuizConfig,
    teamEditor: TeamEditor,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    quizProgressIndicator: QuizProgressIndicator,
    questionComponent: QuestionComponent,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(router: RouterContext): VdomElement = {
    component(Props(router))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config = ComponentConfig(backendConstructor = new Backend(_), initialState = State())
    .withStateStoresDependency(
      teamsAndQuizStateStore,
      _.copy(
        teams = teamsAndQuizStateStore.stateOrEmpty.teams,
        quizState = teamsAndQuizStateStore.stateOrEmpty.quizState,
      ))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(router: RouterContext)
  protected case class State(
      teams: Seq[Team] = Seq(),
      quizState: QuizState = QuizState.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomElement = logExceptions {
      implicit val router = props.router

      <.span(
        ^.className := "master-view",
        quizNavigationButtons(state.quizState),
        quizProgressIndicator(state.quizState),
        state.quizState match {
          case quizState if quizState.quizIsBeingSetUp =>
            teamEditor()
          case quizState =>
            quizState.maybeQuestion match {
              case None =>
                RoundComponent(quizState.round)
              case Some(question) =>
                questionComponent(
                  question = question,
                  round = state.quizState.round,
                  questionProgressIndex = quizState.questionProgressIndex,
                  showMasterData = true,
                )
            }
        },
      )
    }

    private def quizNavigationButtons(quizState: QuizState): VdomTag = {
      <.div(
        ^.className := "quiz-navigation-buttons",
        <<.ifThen(!quizState.quizIsBeingSetUp) {
          <.span(
            Bootstrap.Button(Variant.primary, Size.lg)(
              ^.onClick --> LogExceptionsCallback(teamsAndQuizStateStore.goToPreviousStep()).void,
              Bootstrap.Glyphicon("arrow-left"),
              " Previous",
            ),
            " ",
          )
        },
        <<.ifThen(!quizState.quizHasEnded) {
          Bootstrap.Button(Variant.primary, Size.lg)(
            ^.onClick --> LogExceptionsCallback(teamsAndQuizStateStore.goToNextStep()).void,
            Bootstrap.Glyphicon("arrow-right"),
            " Next",
          )
        }
      )
    }
  }
}
