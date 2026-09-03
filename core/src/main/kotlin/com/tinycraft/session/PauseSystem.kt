package com.tinycraft.session

import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState

/** Consumes pause intent and toggles session state. Storage remains a separate concern. */
class PauseSystem {
    fun update(inputState: InputState, session: GameSessionState): Boolean {
        if (!inputState.consumeAction(GameAction.PAUSE)) return false
        session.paused = !session.paused
        inputState.reset()
        return true
    }
}
