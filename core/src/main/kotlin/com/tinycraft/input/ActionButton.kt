package com.tinycraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.tinycraft.theme.GameDimensions

/** Input-only circular action button. Rendering is owned by TouchHudRenderer. */
abstract class ActionButton(
    private val inputState: InputState,
    private val action: GameAction
) : InputAdapter() {
    private val center = Vector2()
    private var activePointer = -1

    protected abstract fun resolveCenter(screenWidth: Float, screenHeight: Float, out: Vector2): Vector2

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (activePointer != -1) return false
        resolveCenter(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat(), center)
        val radius = GameDimensions.TOUCH_BUTTON_SIZE * 0.5f
        val dx = screenX - center.x
        val dy = screenY - center.y
        if (dx * dx + dy * dy > radius * radius) return false

        activePointer = pointer
        inputState.queueAction(action)
        return true
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        if (pointer != activePointer) return false
        activePointer = -1
        return true
    }
}
