package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import com.tinycraft.blocks.BlockRegistry
import com.tinycraft.config.WorldConfig

/** Owns authoritative loaded chunk state and persistent player-authored modifications. */
class World {
    private data class ModificationKey(val x: Int, val y: Int, val z: Int)

    private val chunks = mutableMapOf<ChunkPosition, Chunk>()
    private val modifications = linkedMapOf<ModificationKey, BlockId>()

    fun getChunk(position: ChunkPosition): Chunk? = chunks[position]

    fun getOrCreateChunk(position: ChunkPosition): Chunk {
        chunks[position]?.let { return it }
        return Chunk(position).also { chunk ->
            chunks[position] = chunk
            markAdjacentChunksDirty(position)
        }
    }

    fun unloadChunk(position: ChunkPosition): Boolean {
        val removed = chunks.remove(position) ?: return false
        @Suppress("UNUSED_VARIABLE")
        val discardedChunk = removed
        markAdjacentChunksDirty(position)
        return true
    }

    fun loadedChunks(): Collection<Chunk> = chunks.values

    fun loadedChunkPositions(): Set<ChunkPosition> = chunks.keys.toSet()

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

    /** Records a persisted override without forcing its chunk to remain loaded. */
    fun recordModification(worldX: Int, worldY: Int, worldZ: Int, blockId: BlockId) {
        require(worldY in 0 until WorldConfig.CHUNK_HEIGHT) { "y outside world: $worldY" }
        modifications[ModificationKey(worldX, worldY, worldZ)] = blockId

        val chunkX = Math.floorDiv(worldX, WorldConfig.CHUNK_WIDTH)
        val chunkZ = Math.floorDiv(worldZ, WorldConfig.CHUNK_DEPTH)
        val chunk = getChunk(ChunkPosition(chunkX, chunkZ)) ?: return
        val localX = Math.floorMod(worldX, WorldConfig.CHUNK_WIDTH)
        val localZ = Math.floorMod(worldZ, WorldConfig.CHUNK_DEPTH)
        chunk.setBlock(localX, worldY, localZ, blockId)
        markBoundaryNeighborsDirty(chunkX, chunkZ, localX, localZ)
    }

    fun modifications(): List<WorldModification> = modifications.map { (key, blockId) ->
        WorldModification(key.x, key.y, key.z, blockId)
    }

    fun modificationsForChunk(position: ChunkPosition): List<WorldModification> {
        return modifications.mapNotNull { (key, blockId) ->
            val chunkX = Math.floorDiv(key.x, WorldConfig.CHUNK_WIDTH)
            val chunkZ = Math.floorDiv(key.z, WorldConfig.CHUNK_DEPTH)
            if (chunkX == position.x && chunkZ == position.z) {
                WorldModification(key.x, key.y, key.z, blockId)
            } else {
                null
            }
        }
    }

    fun findHighestSolidY(worldX: Int, worldZ: Int): Int {
        for (y in WorldConfig.CHUNK_HEIGHT - 1 downTo 0) {
            val blockId = getBlock(worldX, y, worldZ)
            if (blockId != BlockId.AIR && BlockRegistry.get(blockId).solid) return y
        }
        return -1
    }

    fun isSolidBlock(worldX: Int, worldY: Int, worldZ: Int): Boolean {
        val blockId = getBlock(worldX, worldY, worldZ)
        return blockId != BlockId.AIR && BlockRegistry.get(blockId).solid
    }

    private fun markBoundaryNeighborsDirty(chunkX: Int, chunkZ: Int, localX: Int, localZ: Int) {
        if (localX == 0) getChunk(ChunkPosition(chunkX - 1, chunkZ))?.markMeshDirty()
        if (localX == WorldConfig.CHUNK_WIDTH - 1) getChunk(ChunkPosition(chunkX + 1, chunkZ))?.markMeshDirty()
        if (localZ == 0) getChunk(ChunkPosition(chunkX, chunkZ - 1))?.markMeshDirty()
        if (localZ == WorldConfig.CHUNK_DEPTH - 1) getChunk(ChunkPosition(chunkX, chunkZ + 1))?.markMeshDirty()
    }

    private fun markAdjacentChunksDirty(position: ChunkPosition) {
        getChunk(ChunkPosition(position.x - 1, position.z))?.markMeshDirty()
        getChunk(ChunkPosition(position.x + 1, position.z))?.markMeshDirty()
        getChunk(ChunkPosition(position.x, position.z - 1))?.markMeshDirty()
        getChunk(ChunkPosition(position.x, position.z + 1))?.markMeshDirty()
    }
}
