package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import kotlin.test.Test
import kotlin.test.assertEquals

class WorldModificationTest {
    @Test
    fun setBlockTracksFinalPlayerAuthoredValueOncePerCoordinate() {
        val world = World()
        val chunk = world.getOrCreateChunk(ChunkPosition(0, 0))
        chunk.setBlock(1, 5, 1, BlockId.DIRT)

        world.setBlock(1, 5, 1, BlockId.AIR)
        world.setBlock(1, 5, 1, BlockId.STONE)

        val modifications = world.modifications()
        assertEquals(1, modifications.size)
        assertEquals(WorldModification(1, 5, 1, BlockId.STONE), modifications.single())
    }
}
