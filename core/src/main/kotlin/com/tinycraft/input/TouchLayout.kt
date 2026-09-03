package com.tinycraft.input

import com.badlogic.gdx.math.Vector2
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.theme.GameDimensions

/** Single geometry source for touch hit testing and HUD drawing. Coordinates use screen top-left origin. */
object TouchLayout {
    fun joystickCenter(screenHeight: Float, out: Vector2 = Vector2()): Vector2 {
        val radius = GameDimensions.JOYSTICK_DIAMETER * 0.5f
        return out.set(
            GameDimensions.HUD_PADDING + radius,
            screenHeight - GameDimensions.HUD_PADDING - radius
        )
    }

    fun jumpCenter(screenWidth: Float, screenHeight: Float, out: Vector2 = Vector2()): Vector2 {
        val half = GameDimensions.TOUCH_BUTTON_SIZE * 0.5f
        return out.set(
            screenWidth - GameDimensions.HUD_PADDING - half,
            screenHeight - GameDimensions.HUD_PADDING - half
        )
    }

    fun mineCenter(screenWidth: Float, screenHeight: Float, out: Vector2 = Vector2()): Vector2 {
        val size = GameDimensions.TOUCH_BUTTON_SIZE
        val half = size * 0.5f
        return out.set(
            screenWidth - GameDimensions.HUD_PADDING - half,
            screenHeight - GameDimensions.HUD_PADDING - half - size - GameDimensions.HUD_GAP
        )
    }

    fun placeCenter(screenWidth: Float, screenHeight: Float, out: Vector2 = Vector2()): Vector2 {
        val size = GameDimensions.TOUCH_BUTTON_SIZE
        val half = size * 0.5f
        return out.set(
            screenWidth - GameDimensions.HUD_PADDING - half - size - GameDimensions.HUD_GAP,
            screenHeight - GameDimensions.HUD_PADDING - half
        )
    }

    fun pauseCenter(screenWidth: Float, screenHeight: Float, out: Vector2 = Vector2()): Vector2 {
        val half = GameDimensions.TOUCH_BUTTON_SIZE * 0.5f
        return out.set(
            screenWidth - GameDimensions.HUD_PADDING - half,
            GameDimensions.HUD_PADDING + half
        )
    }

    fun hotbarSlotCenter(
        slot: Int,
        screenWidth: Float,
        screenHeight: Float,
        out: Vector2 = Vector2()
    ): Vector2 {
        require(slot in 0 until PlayerInventoryState.HOTBAR_SIZE) { "Invalid hotbar slot: $slot" }
        val size = GameDimensions.HOTBAR_SLOT_SIZE
        val totalWidth = PlayerInventoryState.HOTBAR_SIZE * size +
            (PlayerInventoryState.HOTBAR_SIZE - 1) * GameDimensions.HUD_GAP
        val firstCenterX = (screenWidth - totalWidth) * 0.5f + size * 0.5f
        return out.set(
            firstCenterX + slot * (size + GameDimensions.HUD_GAP),
            screenHeight - GameDimensions.HUD_PADDING - size * 0.5f
        )
    }
}
