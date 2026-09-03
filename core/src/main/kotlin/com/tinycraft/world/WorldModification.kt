package com.tinycraft.world

import com.tinycraft.blocks.BlockId

/** Final player-authored block value layered over deterministic generated terrain. */
data class WorldModification(
    val x: Int,
    val y: Int,
    val z: Int,
    val blockId: BlockId
)
