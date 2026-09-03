package com.tinycraft.rendering

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.utils.Disposable
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World

/** Owns GPU chunk meshes and rebuilds only when a chunk mesh revision changes. */
class ChunkRenderCache(
    private val faceBuilder: ChunkFaceBuilder = ChunkFaceBuilder(),
    private val meshBuilder: ChunkMeshBuilder = ChunkMeshBuilder()
) : Disposable {
    private data class Entry(val revision: Long, val mesh: Mesh?)

    private val entries = mutableMapOf<ChunkPosition, Entry>()

    fun synchronize(world: World) {
        val loadedChunks = world.loadedChunks()
        val loadedPositions = loadedChunks.mapTo(HashSet()) { it.position }

        val unloaded = entries.keys.filter { it !in loadedPositions }
        for (position in unloaded) {
            entries.remove(position)?.mesh?.dispose()
        }

        for (chunk in loadedChunks) {
            val cached = entries[chunk.position]
            if (cached?.revision == chunk.meshRevision) continue

            cached?.mesh?.dispose()
            val visibleFaces = faceBuilder.build(world, chunk)
            entries[chunk.position] = Entry(
                revision = chunk.meshRevision,
                mesh = meshBuilder.build(visibleFaces)
            )
        }
    }

    fun meshes(): Sequence<Mesh> = entries.values.asSequence().mapNotNull(Entry::mesh)

    override fun dispose() {
        entries.values.forEach { it.mesh?.dispose() }
        entries.clear()
    }
}
