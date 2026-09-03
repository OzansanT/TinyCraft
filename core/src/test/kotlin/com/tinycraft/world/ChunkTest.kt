package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunkTest {
    @Test
    fun newChunkStartsAsAir() {
        val chunk = Chunk(ChunkPosition(0, 0))

        assertEquals(BlockId.AIR, chunk.getBlock(0, 0, 0))
    }

    @Test
    fun settingBlockAdvancesMeshRevision() {
        val chunk = Chunk(ChunkPosition(0, 0))
        val initialRevision = chunk.meshRevision

        chunk.setBlock(1, 2, 3, BlockId.STONE)

        assertEquals(BlockId.STONE, chunk.getBlock(1, 2, 3))
        assertTrue(chunk.meshRevision > initialRevision)
    }

    @Test
    fun settingSameBlockDoesNotAdvanceMeshRevisionAgain() {
        val chunk = Chunk(ChunkPosition(0, 0))
        chunk.setBlock(1, 2, 3, BlockId.STONE)
        val revision = chunk.meshRevision

        chunk.setBlock(1, 2, 3, BlockId.STONE)

        assertEquals(revision, chunk.meshRevision)
    }
}
