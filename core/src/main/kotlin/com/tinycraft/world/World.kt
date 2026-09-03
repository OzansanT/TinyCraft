package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import com.tinycraft.config.WorldConfig

/** Owns authoritative chunk state and player-authored modifications. */
class World {
    private data class ModificationKey(val x: Int, val y: Int, val z: Int)

    private val chunks = mutableMapOf<ChunkPosition, Chunk>()
    private val modifications = linkedMapOf<ModificationKey, BlockId>()

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
        if (getBlock(worldX, worldY, worldZ) == blockId) return

        val chunkX = Math.floorDiv(worldX, WorldConfig.CHUNK_WIDTH)
        val chunkZ = Math.floorDiv(worldZ, WorldConfig.CHUNK_DEPTH)
        val localX = Math.floorMod(worldX, WorldConfig.CHUNK_WIDTH)
        val localZ = Math.floorMod(worldZ, WorldConfig.CHUNK_DEPTH)
        val chunk = getOrCreateChunk(ChunkPosition(chunkX, chunkZ))

        chunk.setBlock(localX, worldY, localZ, blockId)
        modifications[ModificationKey(worldX, worldY, worldZ)] = blockId
        markBoundaryNeighborsDirty(chunkX, chunkZ, localX, localZ)
    }

    fun modifications(): List<WorldModification> = modifications.map { (key, blockId) ->
        WorldModification(key.x, key.y, key.z, blockId)
    }

    fun highestSolidBlockY(worldX: Int, worldZ: Int): Int? {
        for (y in WorldConfig.CHUNK_HEIGHT - 1 downTo 0) {
            if (getBlock(worldX, y, worldZ) != BlockId.AIR) return y
        }
        return null
    }

    private fun markBoundaryNeighborsDirty(chunkX: Int, chunkZ: Int, localX: Int, localZ: Int) {
        if (localX == 0) getChunk(ChunkPosition(chunkX - 1, chunkZ))?.markMeshDirty()
        if (localX == WorldConfig.CHUNK_WIDTH - 1) getChunk(ChunkPosition(chunkX + 1, chunkZ))?.markMeshDirty()
        if (localZ == 0) getChunk(ChunkPosition(chunkX, chunkZ - 1))?.markMeshDirty()
        if (localZ == WorldConfig.CHUNK_DEPTH - 1) getChunk(ChunkPosition(chunkX, chunkZ + 1))?.markMeshDirty()
    }
}
