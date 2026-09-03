package com.tinycraft.world.streaming

import com.tinycraft.blocks.BlockId
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World
import com.tinycraft.world.generation.WorldGenerator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChunkStreamingSystemTest {
    @Test
    fun loadsBoundedChunkSquareAroundPlayer() {
        val world = World()
        val streaming = ChunkStreamingSystem(world, WorldGenerator(42L), loadRadius = 1, unloadRadius = 1)

        streaming.update(0.5f, 0.5f)

        assertEquals(9, world.loadedChunks().size)
        assertEquals(ChunkPosition(0, 0), streaming.chunkPositionAt(0.5f, 0.5f))
        assertEquals(ChunkPosition(-1, -1), streaming.chunkPositionAt(-0.5f, -0.5f))
    }

    @Test
    fun unloadedChunkRegeneratesWithPlayerModification() {
        val world = World()
        val generator = WorldGenerator(42L)
        val streaming = ChunkStreamingSystem(world, generator, loadRadius = 1, unloadRadius = 1)

        streaming.update(0.5f, 0.5f)
        world.setBlock(1, 20, 1, BlockId.WOOD)

        streaming.update(160.5f, 0.5f)
        assertNull(world.getChunk(ChunkPosition(0, 0)))

        streaming.update(0.5f, 0.5f)
        assertEquals(BlockId.WOOD, world.getBlock(1, 20, 1))
    }
}
