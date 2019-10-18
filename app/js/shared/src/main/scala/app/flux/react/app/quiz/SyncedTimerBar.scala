package app.flux.react.app.quiz

import java.time.Duration

import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.access.ModelFields
import app.models.quiz.QuizState.TimerState
import hydro.common.time.JavaTimeImplicits._
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.time.Clock
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.Bootstrap.Variant
import hydro.jsfacades.Mousetrap
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom

import scala.concurrent.Future

final class SyncedTimerBar(
    implicit clock: Clock,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(maxTime: Duration): VdomElement = {
    component(Props(maxTime = maxTime))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(timerState = teamsAndQuizStateStore.stateOrEmpty.quizState.timerState))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(maxTime: Duration)
  protected case class State(
      timerState: TimerState = TimerState.nullInstance,
      elapsedTime: Duration = Duration.ZERO,
  )

  protected class Backend($ : BackendScope[Props, State])
      extends BackendBase($)
      with DidMount
      with WillUnmount {

    private var intervalHandle: Option[Int] = None

    override def render(props: Props, state: State): VdomElement = logExceptions {
      val timeRemaining = Seq(props.maxTime - state.elapsedTime, Duration.ZERO).max
      val timeRemainingFraction = timeRemaining / props.maxTime
      Bootstrap.ProgressBar(
        fraction = timeRemainingFraction,
        variant = if (timeRemainingFraction < 0.1) Variant.danger else Variant.info)
    }

    override def didMount(props: Props, state: State): Callback = {
      bindShortcuts()

      intervalHandle = Some(
        dom.window.setInterval(
          () => {
            $.modState(state => state.copy(elapsedTime = state.timerState.elapsedTime())).runNow()
          },
          /* timeout in millis */ 10
        )
      )

      Callback.empty
    }
    override def willUnmount(props: Props, state: State): Callback = {
      for (handle <- intervalHandle) dom.window.clearInterval(handle)
      Callback.empty
    }

    private def bindShortcuts(): Unit = {
      def bind(shortcut: String, runnable: () => Unit): Unit = {
        Mousetrap.bind(shortcut, e => {
          e.preventDefault()
          runnable()
        })
      }
      bind("space", () => togglePaused())
    }

    private def togglePaused(): Future[Unit] = {
      teamsAndQuizStateStore.doQuizStateUpdate(ModelFields.QuizState.timerState) { state =>
        val timerState = state.timerState
        state.copy(
          timerState = TimerState(
            lastSnapshotInstant = clock.nowInstant,
            lastSnapshotElapsedTime = timerState.elapsedTime(),
            timerRunning = !timerState.timerRunning,
          ))
      }
    }
  }
}