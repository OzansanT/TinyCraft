package com.tinycraft.rendering

import com.badlogic.gdx.graphics.Color
import com.tinycraft.blocks.BlockId
import com.tinycraft.theme.GameColors

/** Maps stable block IDs to centralized theme colors used by the first untextured renderer. */
object BlockRenderPalette {
    fun color(blockId: BlockId): Color = when (blockId) {
        BlockId.GRASS -> GameColors.GRASS
        BlockId.DIRT -> GameColors.DIRT
        BlockId.STONE -> GameColors.STONE
        BlockId.SAND -> GameColors.SAND
        BlockId.WOOD -> GameColors.WOOD
        BlockId.LEAVES -> GameColors.LEAVES
        BlockId.WATER -> GameColors.WATER
        BlockId.AIR -> Color.CLEAR
    }
}
