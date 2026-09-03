package com.tinycraft.inventory

import com.tinycraft.blocks.BlockId

/** Immutable block stack value used by the six-slot player hotbar. */
data class ItemStack(
    val blockId: BlockId,
    val count: Int
) {
    init {
        require(blockId != BlockId.AIR) { "AIR cannot be stored in inventory" }
        require(count in 1..MAX_STACK_SIZE) { "Invalid stack count: $count" }
    }

    companion object {
        const val MAX_STACK_SIZE = 64
    }
}
