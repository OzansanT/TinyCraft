package com.tinycraft.player

import com.tinycraft.blocks.BlockId
import com.tinycraft.inventory.ItemStack
import kotlin.math.min

/** Authoritative six-slot hotbar inventory owned by the player. */
class PlayerInventoryState {
    private val slots = MutableList<ItemStack?>(HOTBAR_SIZE) { null }

    var selectedSlotIndex: Int = 0
        private set

    fun slot(index: Int): ItemStack? {
        require(index in 0 until HOTBAR_SIZE) { "Hotbar slot outside range: $index" }
        return slots[index]
    }

    fun selectedStack(): ItemStack? = slots[selectedSlotIndex]

    fun selectSlot(index: Int) {
        require(index in 0 until HOTBAR_SIZE) { "Hotbar slot outside range: $index" }
        selectedSlotIndex = index
    }

    /** Adds blocks to existing stacks first, then empty slots. Returns the un-stored remainder. */
    fun add(blockId: BlockId, amount: Int = 1): Int {
        require(blockId != BlockId.AIR) { "AIR cannot be added to inventory" }
        require(amount >= 0) { "amount must be non-negative" }
        var remainder = amount
        if (remainder == 0) return 0

        for (index in slots.indices) {
            val stack = slots[index] ?: continue
            if (stack.blockId != blockId || stack.count >= ItemStack.MAX_STACK_SIZE) continue

            val added = min(ItemStack.MAX_STACK_SIZE - stack.count, remainder)
            slots[index] = stack.copy(count = stack.count + added)
            remainder -= added
            if (remainder == 0) return 0
        }

        for (index in slots.indices) {
            if (slots[index] != null) continue
            val added = min(ItemStack.MAX_STACK_SIZE, remainder)
            slots[index] = ItemStack(blockId, added)
            remainder -= added
            if (remainder == 0) return 0
        }

        return remainder
    }

    fun addOne(blockId: BlockId): Boolean = add(blockId, 1) == 0

    fun consumeSelected(amount: Int = 1): Boolean {
        require(amount > 0) { "amount must be positive" }
        val stack = slots[selectedSlotIndex] ?: return false
        if (stack.count < amount) return false

        val remaining = stack.count - amount
        slots[selectedSlotIndex] = if (remaining == 0) null else stack.copy(count = remaining)
        return true
    }

    fun snapshot(): List<ItemStack?> = slots.toList()

    fun restore(selectedSlot: Int, restoredSlots: List<ItemStack?>) {
        require(selectedSlot in 0 until HOTBAR_SIZE) { "Invalid selected slot: $selectedSlot" }
        require(restoredSlots.size == HOTBAR_SIZE) { "Expected $HOTBAR_SIZE hotbar slots" }
        for (index in slots.indices) slots[index] = restoredSlots[index]
        selectedSlotIndex = selectedSlot
    }

    companion object {
        const val HOTBAR_SIZE = 6
    }
}
