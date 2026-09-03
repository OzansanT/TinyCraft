package com.tinycraft.input

import com.badlogic.gdx.math.Vector2

class JumpButton(inputState: InputState) : ActionButton(inputState, GameAction.JUMP) {
    override fun resolveCenter(screenWidth: Float, screenHeight: Float, out: Vector2): Vector2 =
        TouchLayout.jumpCenter(screenWidth, screenHeight, out)
}
