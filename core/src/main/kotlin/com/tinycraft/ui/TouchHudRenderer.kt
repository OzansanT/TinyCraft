package com.tinycraft.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.tinycraft.input.InputState
import com.tinycraft.input.TouchLayout
import com.tinycraft.theme.GameColors
import com.tinycraft.theme.GameDimensions

/** Draws touch controls and crosshair. Hit testing remains in the input components. */
class TouchHudRenderer(private val inputState: InputState) : Disposable {
    private val shapes = ShapeRenderer()
    private val projection = Matrix4()
    private val center = Vector2()

    fun render() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        if (width <= 0f || height <= 0f) return

        shapes.projectionMatrix = projection.setToOrtho2D(0f, 0f, width, height)
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        drawFilledControls(width, height)
        drawControlIcons(width, height)
    }

    private fun drawFilledControls(width: Float, height: Float) {
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.color = GameColors.UI_BACKGROUND

        TouchLayout.joystickCenter(height, center)
        val joystickX = center.x
        val joystickY = height - center.y
        val joystickRadius = GameDimensions.JOYSTICK_DIAMETER * 0.5f
        shapes.circle(joystickX, joystickY, joystickRadius, 40)

        shapes.color = GameColors.UI_PRIMARY
        shapes.circle(
            joystickX + inputState.moveX * joystickRadius * 0.55f,
            joystickY + inputState.moveForward * joystickRadius * 0.55f,
            joystickRadius * 0.34f,
            32
        )

        shapes.color = GameColors.UI_BACKGROUND
        drawButtonCircle(TouchLayout.jumpCenter(width, height, center), height)
        drawButtonCircle(TouchLayout.mineCenter(width, height, center), height)
        drawButtonCircle(TouchLayout.placeCenter(width, height, center), height)
        shapes.end()
    }

    private fun drawButtonCircle(screenCenter: Vector2, screenHeight: Float) {
        shapes.circle(
            screenCenter.x,
            screenHeight - screenCenter.y,
            GameDimensions.TOUCH_BUTTON_SIZE * 0.5f,
            32
        )
    }

    private fun drawControlIcons(width: Float, height: Float) {
        Gdx.gl.glLineWidth(GameDimensions.HUD_LINE_WIDTH)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        shapes.color = GameColors.UI_TEXT

        val iconRadius = GameDimensions.TOUCH_BUTTON_SIZE * 0.18f

        TouchLayout.jumpCenter(width, height, center)
        val jumpY = height - center.y
        shapes.triangle(
            center.x,
            jumpY + iconRadius,
            center.x - iconRadius,
            jumpY - iconRadius,
            center.x + iconRadius,
            jumpY - iconRadius
        )

        TouchLayout.mineCenter(width, height, center)
        val mineY = height - center.y
        shapes.line(center.x - iconRadius, mineY - iconRadius, center.x + iconRadius, mineY + iconRadius)
        shapes.line(center.x - iconRadius, mineY + iconRadius, center.x + iconRadius, mineY - iconRadius)

        TouchLayout.placeCenter(width, height, center)
        val placeY = height - center.y
        shapes.rect(center.x - iconRadius, placeY - iconRadius, iconRadius * 2f, iconRadius * 2f)

        val crosshair = GameDimensions.CROSSHAIR_HALF_SIZE
        shapes.line(width * 0.5f - crosshair, height * 0.5f, width * 0.5f + crosshair, height * 0.5f)
        shapes.line(width * 0.5f, height * 0.5f - crosshair, width * 0.5f, height * 0.5f + crosshair)
        shapes.end()
    }

    override fun dispose() {
        shapes.dispose()
    }
}
