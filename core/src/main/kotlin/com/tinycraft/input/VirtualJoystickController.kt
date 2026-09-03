package com.tinycraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.tinycraft.theme.GameDimensions
import kotlin.math.sqrt

/** Left-thumb movement joystick. It only writes normalized movement intent. */
class VirtualJoystickController(private val inputState: InputState) : InputAdapter() {
    private val center = Vector2()
    private var activePointer = -1

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (activePointer != -1) return false
        TouchLayout.joystickCenter(Gdx.graphics.height.toFloat(), center)
        val radius = GameDimensions.JOYSTICK_DIAMETER * 0.6f
        val dx = screenX - center.x
        val dy = screenY - center.y
        if (dx * dx + dy * dy > radius * radius) return false

        activePointer = pointer
        updateMovement(screenX.toFloat(), screenY.toFloat())
        return true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        if (pointer != activePointer) return false
        updateMovement(screenX.toFloat(), screenY.toFloat())
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != activePointer) return false
        activePointer = -1
        inputState.setMovement(0f, 0f)
        return true
    }

    private fun updateMovement(screenX: Float, screenY: Float) {
        TouchLayout.joystickCenter(Gdx.graphics.height.toFloat(), center)
        val radius = GameDimensions.JOYSTICK_DIAMETER * 0.5f
        var x = (screenX - center.x) / radius
        var forward = (center.y - screenY) / radius
        val lengthSquared = x * x + forward * forward
        if (lengthSquared > 1f) {
            val length = sqrt(lengthSquared)
            x /= length
            forward /= length
        }
        inputState.setMovement(x, forward)
    }
}
