package com.tinycraft.nativeandroid

import java.lang.Math.floorDiv
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** CPU-side chunk meshing. Geometry is rebuilt only when blocks in or next to a chunk change. */
data class ChunkCoord(val x: Int, val z: Int)

data class CpuChunkMesh(
    val coord: ChunkCoord,
    val vertices: FloatArray,
    val centerX: Float,
    val centerY: Float,
    val centerZ: Float,
    val radius: Float
) {
    val vertexCount: Int get() = vertices.size / FLOATS_PER_VERTEX

    companion object {
        const val FLOATS_PER_VERTEX = 7 // xyz + rgba
    }
}

object ChunkMesher {
    const val CHUNK_SIZE = 8

    fun chunkOf(x: Int, z: Int): ChunkCoord =
        ChunkCoord(floorDiv(x, CHUNK_SIZE), floorDiv(z, CHUNK_SIZE))

    fun chunksForWorld(world: VoxelWorld): Set<ChunkCoord> =
        world.allBlocks.asSequence().map { chunkOf(it.x, it.z) }.toSet()

    fun affectedChunks(x: Int, z: Int): Set<ChunkCoord> = setOf(
        chunkOf(x, z),
        chunkOf(x - 1, z),
        chunkOf(x + 1, z),
        chunkOf(x, z - 1),
        chunkOf(x, z + 1)
    )

    /**
     * Greedy-mesh all exposed faces belonging to one X/Z chunk.
     * Adjacent blocks across chunk boundaries are queried so hidden boundary faces are removed.
     */
    fun build(world: VoxelWorld, coord: ChunkCoord): CpuChunkMesh {
        val chunkMinX = coord.x * CHUNK_SIZE
        val chunkMinZ = coord.z * CHUNK_SIZE
        val chunkMaxX = chunkMinX + CHUNK_SIZE - 1
        val chunkMaxZ = chunkMinZ + CHUNK_SIZE - 1

        val ownBlocks = world.allBlocks.filter {
            it.x in chunkMinX..chunkMaxX && it.z in chunkMinZ..chunkMaxZ
        }

        if (ownBlocks.isEmpty()) {
            return CpuChunkMesh(
                coord,
                FloatArray(0),
                chunkMinX + (CHUNK_SIZE - 1) * 0.5f,
                0f,
                chunkMinZ + (CHUNK_SIZE - 1) * 0.5f,
                CHUNK_SIZE * 0.75f
            )
        }

        val minY = ownBlocks.minOf { it.y }
        val maxY = ownBlocks.maxOf { it.y }
        val origin = intArrayOf(chunkMinX, minY, chunkMinZ)
        val dims = intArrayOf(CHUNK_SIZE, maxY - minY + 1, CHUNK_SIZE)
        val out = FloatBuilder(max(512, ownBlocks.size * 28))

        for (d in 0..2) {
            val u = (d + 1) % 3
            val v = (d + 2) % 3
            val q = intArrayOf(0, 0, 0)
            q[d] = 1
            val x = intArrayOf(0, 0, 0)
            val mask = arrayOfNulls<FaceCell>(dims[u] * dims[v])

            x[d] = -1
            while (x[d] < dims[d]) {
                var n = 0
                x[v] = 0
                while (x[v] < dims[v]) {
                    x[u] = 0
                    while (x[u] < dims[u]) {
                        val a = blockAt(world, origin, x[0], x[1], x[2])
                        val b = blockAt(world, origin, x[0] + q[0], x[1] + q[1], x[2] + q[2])

                        mask[n++] = when {
                            a != null && b == null && belongsToChunk(a, coord) -> FaceCell(a.type, +1)
                            b != null && a == null && belongsToChunk(b, coord) -> FaceCell(b.type, -1)
                            else -> null
                        }
                        x[u]++
                    }
                    x[v]++
                }

                x[d]++

                n = 0
                var j = 0
                while (j < dims[v]) {
                    var i = 0
                    while (i < dims[u]) {
                        val cell = mask[n]
                        if (cell == null) {
                            i++
                            n++
                            continue
                        }

                        var width = 1
                        while (i + width < dims[u] && mask[n + width] == cell) width++

                        var height = 1
                        heightLoop@ while (j + height < dims[v]) {
                            for (k in 0 until width) {
                                if (mask[n + k + height * dims[u]] != cell) break@heightLoop
                            }
                            height++
                        }

                        x[u] = i
                        x[v] = j
                        emitQuad(out, origin, x, d, u, v, width, height, cell)

                        for (dy in 0 until height) {
                            for (dx in 0 until width) {
                                mask[n + dx + dy * dims[u]] = null
                            }
                        }

                        i += width
                        n += width
                    }
                    j++
                }
            }
        }

        val centerX = chunkMinX + (CHUNK_SIZE - 1) * 0.5f
        val centerY = (minY + maxY) * 0.5f
        val centerZ = chunkMinZ + (CHUNK_SIZE - 1) * 0.5f
        val radius = sqrt(
            (CHUNK_SIZE * CHUNK_SIZE +
                (maxY - minY + 1) * (maxY - minY + 1) +
                CHUNK_SIZE * CHUNK_SIZE).toFloat()
        ) * 0.6f

        return CpuChunkMesh(coord, out.toArray(), centerX, centerY, centerZ, radius)
    }

    private fun blockAt(
        world: VoxelWorld,
        origin: IntArray,
        x: Int,
        y: Int,
        z: Int
    ): Block? = world.blockAt(origin[0] + x, origin[1] + y, origin[2] + z)

    private fun belongsToChunk(block: Block, coord: ChunkCoord): Boolean =
        chunkOf(block.x, block.z) == coord

    private fun emitQuad(
        out: FloatBuilder,
        origin: IntArray,
        x: IntArray,
        d: Int,
        u: Int,
        v: Int,
        width: Int,
        height: Int,
        cell: FaceCell
    ) {
        val p = FloatArray(3) { axis -> origin[axis] + x[axis] - 0.5f }
        val du = FloatArray(3)
        val dv = FloatArray(3)
        du[u] = width.toFloat()
        dv[v] = height.toFloat()

        val p0 = p
        val p1 = add(p, du)
        val p2 = add(p1, dv)
        val p3 = add(p, dv)

        val shade = when (d) {
            1 -> if (cell.sign > 0) 1.08f else 0.70f
            0 -> 0.82f
            else -> 0.92f
        }
        val c = cell.type.color
        val r = min(1f, c[0] * shade)
        val g = min(1f, c[1] * shade)
        val b = min(1f, c[2] * shade)

        if (cell.sign > 0) {
            vertex(out, p0, r, g, b)
            vertex(out, p1, r, g, b)
            vertex(out, p2, r, g, b)
            vertex(out, p0, r, g, b)
            vertex(out, p2, r, g, b)
            vertex(out, p3, r, g, b)
        } else {
            vertex(out, p0, r, g, b)
            vertex(out, p3, r, g, b)
            vertex(out, p2, r, g, b)
            vertex(out, p0, r, g, b)
            vertex(out, p2, r, g, b)
            vertex(out, p1, r, g, b)
        }
    }

    private fun add(a: FloatArray, b: FloatArray) =
        floatArrayOf(a[0] + b[0], a[1] + b[1], a[2] + b[2])

    private fun vertex(out: FloatBuilder, p: FloatArray, r: Float, g: Float, b: Float) {
        out.add(p[0]); out.add(p[1]); out.add(p[2])
        out.add(r); out.add(g); out.add(b); out.add(1f)
    }

    private data class FaceCell(val type: BlockType, val sign: Int)

    private class FloatBuilder(initialCapacity: Int) {
        private var data = FloatArray(initialCapacity)
        private var size = 0

        fun add(value: Float) {
            if (size == data.size) data = data.copyOf(max(16, data.size * 2))
            data[size++] = value
        }

        fun toArray(): FloatArray = data.copyOf(size)
    }
}
