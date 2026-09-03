package com.tinycraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter

/** Right-side drag look. Action buttons get first chance to claim touches in the multiplexer. */
class TouchLookController(private val inputState: InputState) : InputAdapter() {
    private var activePointer = -1
    private var lastX = 0
    private var lastY = 0

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (activePointer != -1 || screenX < Gdx.graphics.width * 0.38f) return false
        activePointer = pointer
        lastX = screenX
        lastY = screenY
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != activePointer) return false
        inputState.addLookDelta((screenX - lastX).toFloat(), (screenY - lastY).toFloat())
        lastX = screenX
        lastY = screenY
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != activePointer) return false
        activePointer = -1
        return true
    }
}
