package com.tinycraft.world

/** Owns authoritative chunk state. Renderers may read this object but may not mutate it. */
class World {
    private val chunks = mutableMapOf<ChunkPosition, Chunk>()

    fun getChunk(position: ChunkPosition): Chunk? = chunks[position]

    fun getOrCreateChunk(position: ChunkPosition): Chunk {
        return chunks.getOrPut(position) { Chunk(position) }
    }

    fun loadedChunks(): Collection<Chunk> = chunks.values
}
