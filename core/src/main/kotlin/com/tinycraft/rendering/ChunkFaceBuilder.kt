package com.tinycraft.rendering

import com.tinycraft.blocks.BlockId
import com.tinycraft.blocks.BlockRegistry
import com.tinycraft.config.WorldConfig
import com.tinycraft.world.Chunk
import com.tinycraft.world.World

/** Pure visibility extraction. Produces only faces that can actually be seen. */
class ChunkFaceBuilder {
    fun build(world: World, chunk: Chunk): List<VisibleFace> {
        val faces = ArrayList<VisibleFace>()
        val originX = chunk.position.x * WorldConfig.CHUNK_WIDTH
        val originZ = chunk.position.z * WorldConfig.CHUNK_DEPTH

        for (localX in 0 until WorldConfig.CHUNK_WIDTH) {
            for (y in 0 until WorldConfig.CHUNK_HEIGHT) {
                for (localZ in 0 until WorldConfig.CHUNK_DEPTH) {
                    val blockId = chunk.getBlock(localX, y, localZ)
                    if (blockId == BlockId.AIR) continue

                    val worldX = originX + localX
                    val worldZ = originZ + localZ
                    for (direction in FaceDirection.entries) {
                        if (shouldRenderFace(world, blockId, worldX, y, worldZ, direction)) {
                            faces += VisibleFace(worldX, y, worldZ, blockId, direction)
                        }
                    }
                }
            }
        }

        return faces
    }

    private fun shouldRenderFace(
        world: World,
        blockId: BlockId,
        x: Int,
        y: Int,
        z: Int,
        direction: FaceDirection
    ): Boolean {
        val neighbor = world.getBlock(x + direction.dx, y + direction.dy, z + direction.dz)
        if (neighbor == BlockId.AIR) return true

        val currentDefinition = BlockRegistry.get(blockId)
        val neighborDefinition = BlockRegistry.get(neighbor)

        if (!neighborDefinition.transparent) return false
        if (neighbor == blockId && currentDefinition.transparent) return false

        return true
    }
}
