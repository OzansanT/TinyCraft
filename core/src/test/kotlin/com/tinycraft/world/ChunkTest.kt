package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChunkTest {
    @Test
    fun newChunkStartsAsAir() {
        val chunk = Chunk(ChunkPosition(0, 0))

        assertEquals(BlockId.AIR, chunk.getBlock(0, 0, 0))
    }

    @Test
    fun settingBlockMarksChunkDirtyUntilMeshIsClean() {
        val chunk = Chunk(ChunkPosition(0, 0))
        chunk.markMeshClean()
        assertFalse(chunk.isDirty)

        chunk.setBlock(1, 2, 3, BlockId.STONE)

        assertEquals(BlockId.STONE, chunk.getBlock(1, 2, 3))
        assertTrue(chunk.isDirty)
    }
}
