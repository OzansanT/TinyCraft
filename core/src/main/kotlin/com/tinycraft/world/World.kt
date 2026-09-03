package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import com.tinycraft.blocks.BlockRegistry
import com.tinycraft.config.WorldConfig

/** Owns authoritative chunk state. Renderers may read this object but may not mutate it. */
class World {
    private val chunks = mutableMapOf<ChunkPosition, Chunk>()

    fun getChunk(position: ChunkPosition): Chunk? = chunks[position]

    fun getOrCreateChunk(position: ChunkPosition): Chunk {
        return chunks.getOrPut(position) { Chunk(position) }
    }

    fun loadedChunks(): Collection<Chunk> = chunks.values

    fun getBlock(worldX: Int, worldY: Int, worldZ: Int): BlockId {
        if (worldY !in 0 until WorldConfig.CHUNK_HEIGHT) return BlockId.AIR

        val chunkX = Math.floorDiv(worldX, WorldConfig.CHUNK_WIDTH)
        val chunkZ = Math.floorDiv(worldZ, WorldConfig.CHUNK_DEPTH)
        val chunk = getChunk(ChunkPosition(chunkX, chunkZ)) ?: return BlockId.AIR

        return chunk.getBlock(
            Math.floorMod(worldX, WorldConfig.CHUNK_WIDTH),
            worldY,
            Math.floorMod(worldZ, WorldConfig.CHUNK_DEPTH)
        )
    }

    fun setBlock(worldX: Int, worldY: Int, worldZ: Int, blockId: BlockId) {
        require(worldY in 0 until WorldConfig.CHUNK_HEIGHT) { "y outside world: $worldY" }

        val chunkX = Math.floorDiv(worldX, WorldConfig.CHUNK_WIDTH)
        val chunkZ = Math.floorDiv(worldZ, WorldConfig.CHUNK_DEPTH)
        val localX = Math.floorMod(worldX, WorldConfig.CHUNK_WIDTH)
        val localZ = Math.floorMod(worldZ, WorldConfig.CHUNK_DEPTH)
        val chunk = getOrCreateChunk(ChunkPosition(chunkX, chunkZ))

        chunk.setBlock(localX, worldY, localZ, blockId)
        markBoundaryNeighborsDirty(chunkX, chunkZ, localX, localZ)
    }

    fun findHighestSolidY(worldX: Int, worldZ: Int): Int {
        for (y in WorldConfig.CHUNK_HEIGHT - 1 downTo 0) {
            val blockId = getBlock(worldX, y, worldZ)
            if (blockId != BlockId.AIR && BlockRegistry.get(blockId).solid) return y
        }
        return -1
    }

    private fun markBoundaryNeighborsDirty(chunkX: Int, chunkZ: Int, localX: Int, localZ: Int) {
        if (localX == 0) getChunk(ChunkPosition(chunkX - 1, chunkZ))?.markMeshDirty()
        if (localX == WorldConfig.CHUNK_WIDTH - 1) getChunk(ChunkPosition(chunkX + 1, chunkZ))?.markMeshDirty()
        if (localZ == 0) getChunk(ChunkPosition(chunkX, chunkZ - 1))?.markMeshDirty()
        if (localZ == WorldConfig.CHUNK_DEPTH - 1) getChunk(ChunkPosition(chunkX, chunkZ + 1))?.markMeshDirty()
    }
}
