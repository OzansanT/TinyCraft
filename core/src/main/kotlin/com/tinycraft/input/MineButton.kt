package com.tinycraft.input

import com.badlogic.gdx.math.Vector2

class MineButton(inputState: InputState) : ActionButton(inputState, GameAction.MINE) {
    override fun resolveCenter(screenWidth: Float, screenHeight: Float, out: Vector2): Vector2 =
        TouchLayout.mineCenter(screenWidth, screenHeight, out)
}
