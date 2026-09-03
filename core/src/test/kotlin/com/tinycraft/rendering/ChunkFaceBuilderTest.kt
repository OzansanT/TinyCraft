package com.tinycraft.rendering

import com.tinycraft.blocks.BlockId
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals

class ChunkFaceBuilderTest {
    private val builder = ChunkFaceBuilder()

    @Test
    fun isolatedBlockProducesSixFaces() {
        val world = World()
        world.setBlock(1, 1, 1, BlockId.STONE)
        val chunk = requireNotNull(world.getChunk(ChunkPosition(0, 0)))

        assertEquals(6, builder.build(world, chunk).size)
    }

    @Test
    fun adjacentBlocksCullSharedFaces() {
        val world = World()
        world.setBlock(1, 1, 1, BlockId.STONE)
        world.setBlock(2, 1, 1, BlockId.STONE)
        val chunk = requireNotNull(world.getChunk(ChunkPosition(0, 0)))

        assertEquals(10, builder.build(world, chunk).size)
    }

    @Test
    fun chunkBoundaryFacesCullAgainstNeighborChunk() {
        val world = World()
        world.setBlock(15, 1, 1, BlockId.STONE)
        world.setBlock(16, 1, 1, BlockId.STONE)
        val left = requireNotNull(world.getChunk(ChunkPosition(0, 0)))
        val right = requireNotNull(world.getChunk(ChunkPosition(1, 0)))

        assertEquals(5, builder.build(world, left).size)
        assertEquals(5, builder.build(world, right).size)
    }
}
