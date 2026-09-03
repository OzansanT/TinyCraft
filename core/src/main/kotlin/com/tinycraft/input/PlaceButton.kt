package com.tinycraft.input

import com.badlogic.gdx.math.Vector2

class PlaceButton(inputState: InputState) : ActionButton(inputState, GameAction.PLACE) {
    override fun resolveCenter(screenWidth: Float, screenHeight: Float, out: Vector2): Vector2 =
        TouchLayout.placeCenter(screenWidth, screenHeight, out)
}
