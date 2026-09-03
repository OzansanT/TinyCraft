package com.tinycraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.math.Vector2
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.theme.GameDimensions

/** Converts hotbar taps into slot-selection intent without mutating inventory directly. */
class HotbarController(private val inputState: InputState) : InputAdapter() {
    private val center = Vector2()

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        val half = GameDimensions.HOTBAR_SLOT_SIZE * 0.5f

        for (slot in 0 until PlayerInventoryState.HOTBAR_SIZE) {
            TouchLayout.hotbarSlotCenter(slot, width, height, center)
            if (screenX >= center.x - half && screenX <= center.x + half &&
                screenY >= center.y - half && screenY <= center.y + half
            ) {
                inputState.queueHotbarSelection(slot)
                return true
            }
        }
        return false
    }
}
