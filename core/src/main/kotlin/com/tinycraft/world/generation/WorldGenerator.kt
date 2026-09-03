package com.tinycraft.world.generation

import com.tinycraft.blocks.BlockId
import com.tinycraft.config.WorldConfig
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World
import kotlin.math.roundToInt

/** Generates deterministic base terrain and reapplies persistent player modifications. */
class WorldGenerator(private val seed: Long) {
    fun generateInitialWorld(
        world: World,
        radiusChunks: Int = WorldConfig.INITIAL_WORLD_RADIUS_CHUNKS
    ) {
        require(radiusChunks >= 0) { "radiusChunks must not be negative" }

        for (chunkX in -radiusChunks..radiusChunks) {
            for (chunkZ in -radiusChunks..radiusChunks) {
                generateChunk(world, ChunkPosition(chunkX, chunkZ))
            }
        }
    }

    fun generateChunk(world: World, position: ChunkPosition) {
        if (world.getChunk(position) != null) return

        val chunk = world.getOrCreateChunk(position)
        val originX = position.x * WorldConfig.CHUNK_WIDTH
        val originZ = position.z * WorldConfig.CHUNK_DEPTH

        for (localX in 0 until WorldConfig.CHUNK_WIDTH) {
            for (localZ in 0 until WorldConfig.CHUNK_DEPTH) {
                val worldX = originX + localX
                val worldZ = originZ + localZ
                val surfaceY = terrainHeightAt(worldX, worldZ)
                val beach = surfaceY <= WorldConfig.SEA_LEVEL + 1

                for (y in 0 until WorldConfig.CHUNK_HEIGHT) {
                    val block = when {
                        y > surfaceY && y <= WorldConfig.SEA_LEVEL -> BlockId.WATER
                        y > surfaceY -> BlockId.AIR
                        y == surfaceY -> if (beach) BlockId.SAND else BlockId.GRASS
                        y >= surfaceY - 3 -> if (beach) BlockId.SAND else BlockId.DIRT
                        else -> BlockId.STONE
                    }
                    chunk.setBlock(localX, y, localZ, block)
                }
            }
        }

        world.modificationsForChunk(position).forEach { edit ->
            chunk.setBlock(
                Math.floorMod(edit.x, WorldConfig.CHUNK_WIDTH),
                edit.y,
                Math.floorMod(edit.z, WorldConfig.CHUNK_DEPTH),
                edit.blockId
            )
        }
    }

    fun terrainHeightAt(worldX: Int, worldZ: Int): Int {
        val broad = TerrainNoise.sample(worldX, worldZ, seed, scale = 32)
        val detail = TerrainNoise.sample(worldX, worldZ, seed xor DETAIL_SEED_OFFSET, scale = 9)
        val combined = broad * 0.72f + detail * 0.28f
        val centered = (combined - 0.5f) * 2f

        return (WorldConfig.BASE_TERRAIN_HEIGHT + centered * WorldConfig.TERRAIN_AMPLITUDE)
            .roundToInt()
            .coerceIn(2, WorldConfig.CHUNK_HEIGHT - 2)
    }

    companion object {
        private const val DETAIL_SEED_OFFSET = 0x51A7E2D3L
    }
}
