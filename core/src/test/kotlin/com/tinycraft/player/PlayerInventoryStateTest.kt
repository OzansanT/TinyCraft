package com.tinycraft.player

import com.tinycraft.blocks.BlockId
import com.tinycraft.inventory.ItemStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerInventoryStateTest {
    @Test
    fun stacksAcrossSlotsAndConsumesSelected() {
        val inventory = PlayerInventoryState()

        assertEquals(0, inventory.add(BlockId.DIRT, ItemStack.MAX_STACK_SIZE + 1))
        assertEquals(ItemStack.MAX_STACK_SIZE, inventory.slot(0)?.count)
        assertEquals(1, inventory.slot(1)?.count)

        inventory.selectSlot(1)
        inventory.consumeSelected()
        assertNull(inventory.slot(1))
    }

    @Test
    fun restorePreservesSelectionAndContents() {
        val inventory = PlayerInventoryState()
        val slots = MutableList<ItemStack?>(PlayerInventoryState.HOTBAR_SIZE) { null }
        slots[3] = ItemStack(BlockId.STONE, 12)

        inventory.restore(3, slots)

        assertEquals(3, inventory.selectedSlotIndex)
        assertEquals(BlockId.STONE, inventory.selectedStack()?.blockId)
        assertEquals(12, inventory.selectedStack()?.count)
    }
}
