package com.tinycraft.player

import com.tinycraft.config.PlayerConfig
import com.tinycraft.world.World
import kotlin.math.floor
import kotlin.math.min

/** Resolves the player's vertical capsule-like box as an axis-aligned voxel AABB. */
class PlayerCollisionSystem(private val world: World) {
    fun moveHorizontal(player: PlayerState, deltaX: Float, deltaZ: Float) {
        if (deltaX != 0f) moveHorizontalAxis(player, deltaX, xAxis = true)
        if (deltaZ != 0f) moveHorizontalAxis(player, deltaZ, xAxis = false)
        player.grounded = isGrounded(player)
    }

    fun moveVertical(player: PlayerState, deltaY: Float) {
        if (deltaY == 0f) {
            player.grounded = isGrounded(player)
            return
        }

        val startY = player.position.y
        val targetY = startY + deltaY
        if (!collidesAt(player.position.x, targetY, player.position.z)) {
            player.position.y = targetY
            player.grounded = isGrounded(player)
            return
        }

        val fraction = maxSafeFraction { fractionValue ->
            !collidesAt(player.position.x, startY + deltaY * fractionValue, player.position.z)
        }
        player.position.y = startY + deltaY * fraction
        player.velocity.y = 0f
        player.grounded = deltaY < 0f || isGrounded(player)
    }

    fun isGrounded(player: PlayerState): Boolean {
        return collidesAt(player.position.x, player.position.y - GROUND_PROBE, player.position.z)
    }

    fun collidesAt(x: Float, y: Float, z: Float): Boolean {
        val minX = x - PlayerConfig.PLAYER_RADIUS + EPSILON
        val maxX = x + PlayerConfig.PLAYER_RADIUS - EPSILON
        val minY = y + EPSILON
        val maxY = y + PlayerConfig.PLAYER_HEIGHT - EPSILON
        val minZ = z - PlayerConfig.PLAYER_RADIUS + EPSILON
        val maxZ = z + PlayerConfig.PLAYER_RADIUS - EPSILON

        for (blockX in floor(minX.toDouble()).toInt()..floor(maxX.toDouble()).toInt()) {
            for (blockY in floor(minY.toDouble()).toInt()..floor(maxY.toDouble()).toInt()) {
                for (blockZ in floor(minZ.toDouble()).toInt()..floor(maxZ.toDouble()).toInt()) {
                    if (world.isSolidBlock(blockX, blockY, blockZ)) return true
                }
            }
        }
        return false
    }

    private fun moveHorizontalAxis(player: PlayerState, delta: Float, xAxis: Boolean) {
        val startX = player.position.x
        val startZ = player.position.z
        val targetX = if (xAxis) startX + delta else startX
        val targetZ = if (xAxis) startZ else startZ + delta

        if (!collidesAt(targetX, player.position.y, targetZ)) {
            player.position.set(targetX, player.position.y, targetZ)
            return
        }

        if (player.grounded && tryStepUp(player, targetX, targetZ)) return

        val fraction = maxSafeFraction { fractionValue ->
            val testX = if (xAxis) startX + delta * fractionValue else startX
            val testZ = if (xAxis) startZ else startZ + delta * fractionValue
            !collidesAt(testX, player.position.y, testZ)
        }
        if (xAxis) player.position.x = startX + delta * fraction
        else player.position.z = startZ + delta * fraction
    }

    private fun tryStepUp(player: PlayerState, targetX: Float, targetZ: Float): Boolean {
        val stepHeight = min(1f, PlayerConfig.MAX_STEP_HEIGHT)
        val raisedY = player.position.y + stepHeight
        if (collidesAt(player.position.x, raisedY, player.position.z)) return false
        if (collidesAt(targetX, raisedY, targetZ)) return false

        player.position.set(targetX, raisedY, targetZ)
        player.grounded = isGrounded(player)
        return true
    }

    private fun maxSafeFraction(isSafe: (Float) -> Boolean): Float {
        var low = 0f
        var high = 1f
        repeat(BINARY_SEARCH_STEPS) {
            val mid = (low + high) * 0.5f
            if (isSafe(mid)) low = mid else high = mid
        }
        return low
    }

    companion object {
        private const val EPSILON = 0.001f
        private const val GROUND_PROBE = 0.04f
        private const val BINARY_SEARCH_STEPS = 10
    }
}
