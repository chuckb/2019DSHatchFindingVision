import com.github.oxo42.stateless4j.StateMachine;
import com.github.oxo42.stateless4j.StateMachineConfig;
import com.github.oxo42.stateless4j.delegates.Action1;
import com.github.oxo42.stateless4j.delegates.FuncBoolean;
import com.github.oxo42.stateless4j.transitions.Transition;

/**
 * Implements a state machine to enforce proper sequencing
 * and execution of the heads up display as the user interacts
 * with the system.
 */
public class HeadsUpDisplayStateMachine {
  private final StateMachine<State, Trigger> stateMachine;
  private int tiltPct;
  private int panPct;

  public HeadsUpDisplayStateMachine(HeadsUpDisplay hud) {
    this(hud, new StateMachine<>(State.IdentifyingTargets, GetConfig(hud)));
  }

  public HeadsUpDisplayStateMachine(HeadsUpDisplay hud, StateMachine<State, Trigger> stateMachine) {
    this.stateMachine = stateMachine;
    this.tiltPct = 0;
    this.panPct = 0;
  }

  /**
   * Produces the configuration of the state machine so that it can be instantiated.
   * 
   * @param hud   A reference to the HeadsUpDisplay so that the state machine
   *              can make calls as appropriate as state change.
   * @return      The configuration.
   */
  private static StateMachineConfig<State, Trigger> GetConfig(HeadsUpDisplay hud) {
    StateMachineConfig<State, Trigger> config = new StateMachineConfig<>();

    // It would be super nice to use the permitIfOtherwiseIgnore function,
    // but alas it is not available in a release version.
    config.configure(State.IdentifyingTargets)
      .onEntry(new Action1<Transition<State,Trigger>>() {
        public void doIt(Transition<State, Trigger> transition) {
          //TODO: Bad, bad, bad! How should we notify the caller?
          try{hud.stopSlewing();}
          catch(Exception e){}
      }})
      .permit(Trigger.Pan, State.Panning)
      .permit(Trigger.Tilt, State.Tilting)
      .permit(Trigger.LeftThumbstickButton, State.Centering)
      .permit(Trigger.LeftShoulderButton, State.Calibrating)
      .permitIf(Trigger.AButton, State.SlewingToTarget, new FuncBoolean() {
        @Override
        public boolean call() {
          return hud.isSlewPointDefined(Trigger.AButton);
        }
      })
      .permitIf(Trigger.BButton, State.SlewingToTarget, new FuncBoolean() {
        @Override
        public boolean call() {
          return hud.isSlewPointDefined(Trigger.BButton);
        }
      })
      .permitIf(Trigger.XButton, State.SlewingToTarget, new FuncBoolean() {
        @Override
        public boolean call() {
          return hud.isSlewPointDefined(Trigger.XButton);
        }
      })
      .permitIf(Trigger.YButton, State.SlewingToTarget, new FuncBoolean() {
        @Override
        public boolean call() {
          return hud.isSlewPointDefined(Trigger.YButton);
        }
      })
      .ignoreIf(Trigger.AButton, new FuncBoolean() {
        @Override
        public boolean call() {
          return !hud.isSlewPointDefined(Trigger.AButton);
        }
      })
      .ignoreIf(Trigger.BButton, new FuncBoolean() {
        @Override
        public boolean call() {
          return !hud.isSlewPointDefined(Trigger.BButton);
        }
      })
      .ignoreIf(Trigger.XButton, new FuncBoolean() {
        @Override
        public boolean call() {
          return !hud.isSlewPointDefined(Trigger.XButton);
        }
      })
      .ignoreIf(Trigger.YButton, new FuncBoolean() {
        @Override
        public boolean call() {
          return !hud.isSlewPointDefined(Trigger.YButton);
        }
      });
    
    config.configure(State.Panning)
      .permit(Trigger.Tilt, State.Tilting)
      .permitReentry(Trigger.Pan)
      .permit(Trigger.LeftThumbstickButton, State.Centering)
      .permit(Trigger.IdentifyTargets, State.IdentifyingTargets)
      .ignore(Trigger.LeftShoulderButton)
      .ignore(Trigger.AButton)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton);

    config.configure(State.Tilting)
      .permit(Trigger.Pan, State.Panning)
      .permitReentry(Trigger.Tilt)
      .permit(Trigger.LeftThumbstickButton, State.Centering)
      .permit(Trigger.IdentifyTargets, State.IdentifyingTargets)
      .ignore(Trigger.LeftShoulderButton)
      .ignore(Trigger.AButton)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton);

    config.configure(State.Centering)
      .permit(Trigger.Tilt, State.Tilting)
      .permit(Trigger.Pan, State.Panning)
      .permitReentry(Trigger.LeftThumbstickButton)
      .permit(Trigger.IdentifyTargets, State.IdentifyingTargets)
      .ignore(Trigger.LeftShoulderButton)
      .ignore(Trigger.AButton)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton);

    config.configure(State.SlewingToTarget)
      .onEntry(new Action1<Transition<State,Trigger>>() {
        public void doIt(Transition<State, Trigger> transition) {
          hud.setSlewPoint(transition.getTrigger());
      }})
      .permit(Trigger.LockOn, State.TargetLocked)
      .permit(Trigger.FailedToLock, State.LockFailed)
      .permit(Trigger.AButton, State.IdentifyingTargets)
      .permit(Trigger.Pan, State.Panning)
      .permit(Trigger.Tilt, State.Tilting)
      .permit(Trigger.LeftThumbstickButton, State.Centering)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton)
      .ignore(Trigger.LeftShoulderButton);

    config.configure(State.TargetLocked)
      .permit(Trigger.BButton, State.DrivingToTarget)
      .permit(Trigger.AButton, State.IdentifyingTargets)
      .permit(Trigger.LoseLock, State.LockLost)
      .permit(Trigger.Pan, State.Panning)
      .permit(Trigger.Tilt, State.Tilting)
      .permit(Trigger.LeftThumbstickButton, State.Centering)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton)
      .ignore(Trigger.LeftShoulderButton);

    config.configure(State.LockFailed)
      .permit(Trigger.IdentifyTargets, State.IdentifyingTargets)
      .ignore(Trigger.Pan)
      .ignore(Trigger.Tilt)
      .ignore(Trigger.LeftThumbstickButton)
      .ignore(Trigger.AButton)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton)
      .ignore(Trigger.LeftShoulderButton);

    config.configure(State.LockLost)
      .permit(Trigger.IdentifyTargets, State.IdentifyingTargets)
      .ignore(Trigger.Pan)
      .ignore(Trigger.Tilt)
      .ignore(Trigger.LeftThumbstickButton)
      .ignore(Trigger.AButton)
      .ignore(Trigger.BButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton)
      .ignore(Trigger.LeftShoulderButton);

    config.configure(State.DrivingToTarget)
      .permit(Trigger.LoseLock, State.LockLost)
      .permit(Trigger.BButton, State.TargetLocked)
      .permit(Trigger.AButton, State.IdentifyingTargets)
      .ignore(Trigger.Pan)
      .ignore(Trigger.Tilt)
      .ignore(Trigger.LeftThumbstickButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton)
      .ignore(Trigger.LeftShoulderButton);

    config.configure(State.Calibrating)
      .permit(Trigger.AButton, State.IdentifyingTargets)
      .ignore(Trigger.BButton)
      .ignore(Trigger.Pan)
      .ignore(Trigger.Tilt)
      .ignore(Trigger.LeftThumbstickButton)
      .ignore(Trigger.LeftShoulderButton)
      .ignore(Trigger.XButton)
      .ignore(Trigger.YButton);

    return config;
  }

  public void aButtonPressed() {
    stateMachine.fire(Trigger.AButton);
  }

  public void bButtonPressed() {
    stateMachine.fire(Trigger.BButton);
  }

  public void xButtonPressed() {
    stateMachine.fire(Trigger.XButton);
  }

  public void yButtonPressed() {
    stateMachine.fire(Trigger.YButton);
  }

  public void tilt(int tiltPct) {
    this.tiltPct = tiltPct;
    stateMachine.fire(Trigger.Tilt);
  }

  public int getTiltPct() {
    return tiltPct;
  }

  public void pan(int panPct) {
    this.panPct = panPct;
    stateMachine.fire(Trigger.Pan);
  }

  public int getPanPct() {
    return panPct;
  }

  public void leftThumbstickButtonPressed() {
    stateMachine.fire(Trigger.LeftThumbstickButton);
  }

  public void identifyTargets() {
    stateMachine.fire(Trigger.IdentifyTargets);
  }

  public State getState() {
    return stateMachine.getState();
  }

  public void failedToLock() {
    stateMachine.fire(Trigger.FailedToLock);
  }

  public void lockOn() {
    stateMachine.fire(Trigger.LockOn);
  }

  public void loseLock() {
    stateMachine.fire(Trigger.LoseLock);
  }

  public void leftShoulderButtonPressed() {
    stateMachine.fire(Trigger.LeftShoulderButton);
  }

  /**
   * The valid states of the state machine.
   */
  public enum State {
    IdentifyingTargets, SlewingToTarget, TargetLocked, LockFailed, LockLost, DrivingToTarget, Panning, Tilting, Centering, Calibrating
  }

  /**
   * Triggers that cause state transitions.
   */
  public enum Trigger {
    AButton, BButton, XButton, YButton, LockOn, FailedToLock, LoseLock, IdentifyTargets, Pan, Tilt, LeftThumbstickButton, LeftShoulderButton
  }
}