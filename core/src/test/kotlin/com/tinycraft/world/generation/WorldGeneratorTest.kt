package com.tinycraft.world.generation

import com.tinycraft.config.WorldConfig
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorldGeneratorTest {
    @Test
    fun sameSeedProducesSameTerrainHeights() {
        val first = WorldGenerator(123_456L)
        val second = WorldGenerator(123_456L)
        val coordinates = listOf(
            0 to 0,
            7 to 19,
            -12 to 4,
            31 to -28,
            -45 to -33
        )

        for ((x, z) in coordinates) {
            assertEquals(first.terrainHeightAt(x, z), second.terrainHeightAt(x, z))
        }
    }

    @Test
    fun initialWorldGeneratesConfiguredChunkNeighborhood() {
        val world = World()
        WorldGenerator(WorldConfig.DEFAULT_WORLD_SEED).generateInitialWorld(world)

        val diameter = WorldConfig.INITIAL_WORLD_RADIUS_CHUNKS * 2 + 1
        assertEquals(diameter * diameter, world.loadedChunks().size)
        assertTrue(world.loadedChunks().all { it.meshRevision > 0L })
    }
}
