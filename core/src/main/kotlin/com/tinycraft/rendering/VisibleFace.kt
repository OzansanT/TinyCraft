package com.tinycraft.rendering

import com.tinycraft.blocks.BlockId

data class VisibleFace(
    val worldX: Int,
    val worldY: Int,
    val worldZ: Int,
    val blockId: BlockId,
    val direction: FaceDirection
)
