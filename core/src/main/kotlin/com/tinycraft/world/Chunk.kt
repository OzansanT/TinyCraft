package com.tinycraft.world

import com.tinycraft.blocks.BlockId
import com.tinycraft.config.WorldConfig

/** Compact authoritative block storage for one chunk. Rendering data is kept elsewhere. */
class Chunk(val position: ChunkPosition) {
    private val blocks = ByteArray(
        WorldConfig.CHUNK_WIDTH * WorldConfig.CHUNK_HEIGHT * WorldConfig.CHUNK_DEPTH
    ) { BlockId.AIR.value.toByte() }

    var isDirty: Boolean = true
        private set

    fun getBlock(x: Int, y: Int, z: Int): BlockId {
        return BlockId.fromValue(blocks[indexOf(x, y, z)].toInt() and 0xFF)
    }

    fun setBlock(x: Int, y: Int, z: Int, blockId: BlockId) {
        val index = indexOf(x, y, z)
        if ((blocks[index].toInt() and 0xFF) == blockId.value) return

        blocks[index] = blockId.value.toByte()
        isDirty = true
    }

    fun markMeshDirty() {
        isDirty = true
    }

    fun markMeshClean() {
        isDirty = false
    }

    private fun indexOf(x: Int, y: Int, z: Int): Int {
        require(x in 0 until WorldConfig.CHUNK_WIDTH) { "x outside chunk: $x" }
        require(y in 0 until WorldConfig.CHUNK_HEIGHT) { "y outside chunk: $y" }
        require(z in 0 until WorldConfig.CHUNK_DEPTH) { "z outside chunk: $z" }

        return x + WorldConfig.CHUNK_WIDTH * (z + WorldConfig.CHUNK_DEPTH * y)
    }
}
