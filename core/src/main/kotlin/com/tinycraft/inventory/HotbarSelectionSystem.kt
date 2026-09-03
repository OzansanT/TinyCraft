package com.tinycraft.inventory

import com.tinycraft.input.InputState
import com.tinycraft.player.PlayerInventoryState

/** Applies platform-neutral slot-selection intent to player-owned inventory state. */
class HotbarSelectionSystem {
    fun update(inputState: InputState, inventory: PlayerInventoryState) {
        val slot = inputState.consumeHotbarSelection() ?: return
        if (slot in 0 until PlayerInventoryState.HOTBAR_SIZE) inventory.selectSlot(slot)
    }
}
