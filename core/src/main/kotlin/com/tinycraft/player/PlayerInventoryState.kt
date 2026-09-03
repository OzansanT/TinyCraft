package com.tinycraft.player

import com.tinycraft.blocks.BlockId

/** Owns the currently selected build block until the full inventory/hotbar milestone arrives. */
class PlayerInventoryState {
    var selectedBlock: BlockId = BlockId.DIRT
        internal set
}
