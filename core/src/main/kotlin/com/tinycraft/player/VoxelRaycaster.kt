package com.tinycraft.player

import com.badlogic.gdx.math.Vector3
import com.tinycraft.blocks.BlockId
import com.tinycraft.config.PlayerConfig
import com.tinycraft.world.World
import kotlin.math.floor

/** Pure voxel targeting helper used by mining and placement systems. */
class VoxelRaycaster(private val world: World) {
    private val direction = Vector3()
    private val sample = Vector3()

    fun raycast(origin: Vector3, rayDirection: Vector3, maxDistance: Float = PlayerConfig.BLOCK_REACH): BlockTarget? {
        if (rayDirection.len2() == 0f) return null
        direction.set(rayDirection).nor()

        var previousX = floor(origin.x.toDouble()).toInt()
        var previousY = floor(origin.y.toDouble()).toInt()
        var previousZ = floor(origin.z.toDouble()).toInt()
        var distance = 0f

        while (distance <= maxDistance) {
            sample.set(direction).scl(distance).add(origin)
            val blockX = floor(sample.x.toDouble()).toInt()
            val blockY = floor(sample.y.toDouble()).toInt()
            val blockZ = floor(sample.z.toDouble()).toInt()
            val blockId = world.getBlock(blockX, blockY, blockZ)

            if (blockId != BlockId.AIR) {
                return BlockTarget(
                    blockX,
                    blockY,
                    blockZ,
                    previousX,
                    previousY,
                    previousZ,
                    blockId
                )
            }

            previousX = blockX
            previousY = blockY
            previousZ = blockZ
            distance += PlayerConfig.RAYCAST_STEP_BLOCKS
        }
        return null
    }
}
