package com.tinycraft.player

import com.tinycraft.blocks.BlockId

data class BlockTarget(
    val blockX: Int,
    val blockY: Int,
    val blockZ: Int,
    val adjacentX: Int,
    val adjacentY: Int,
    val adjacentZ: Int,
    val blockId: BlockId
)
