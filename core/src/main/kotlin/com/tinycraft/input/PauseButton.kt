package com.tinycraft.input

import com.badlogic.gdx.math.Vector2

/** Top-right pause/resume input component. Gameplay behavior remains in PauseSystem. */
class PauseButton(inputState: InputState) : ActionButton(inputState, GameAction.PAUSE) {
    override fun resolveCenter(screenWidth: Float, screenHeight: Float, out: Vector2): Vector2 =
        TouchLayout.pauseCenter(screenWidth, screenHeight, out)
}
