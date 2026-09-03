package com.tinycraft.world.streaming

import com.tinycraft.config.WorldConfig
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World
import com.tinycraft.world.generation.WorldGenerator
import kotlin.math.abs
import kotlin.math.floor

/** Keeps a bounded square of deterministic chunks loaded around the player. */
class ChunkStreamingSystem(
    private val world: World,
    private val generator: WorldGenerator,
    private val loadRadius: Int = WorldConfig.STREAM_RADIUS_CHUNKS,
    private val unloadRadius: Int = WorldConfig.UNLOAD_RADIUS_CHUNKS
) {
    init {
        require(loadRadius >= 0) { "loadRadius must not be negative" }
        require(unloadRadius >= loadRadius) { "unloadRadius must be >= loadRadius" }
    }

    fun update(playerX: Float, playerZ: Float) {
        val center = chunkPositionAt(playerX, playerZ)

        for (chunkX in center.x - loadRadius..center.x + loadRadius) {
            for (chunkZ in center.z - loadRadius..center.z + loadRadius) {
                val position = ChunkPosition(chunkX, chunkZ)
                if (world.getChunk(position) == null) {
                    generator.generateChunk(world, position)
                }
            }
        }

        world.loadedChunkPositions().forEach { position ->
            val outsideUnloadRadius =
                abs(position.x - center.x) > unloadRadius || abs(position.z - center.z) > unloadRadius
            if (outsideUnloadRadius) world.unloadChunk(position)
        }
    }

    fun chunkPositionAt(worldX: Float, worldZ: Float): ChunkPosition {
        val blockX = floor(worldX.toDouble()).toInt()
        val blockZ = floor(worldZ.toDouble()).toInt()
        return ChunkPosition(
            Math.floorDiv(blockX, WorldConfig.CHUNK_WIDTH),
            Math.floorDiv(blockZ, WorldConfig.CHUNK_DEPTH)
        )
    }
}
