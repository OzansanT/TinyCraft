package com.tinycraft.player

import com.badlogic.gdx.math.MathUtils
import com.tinycraft.config.PlayerConfig
import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState
import com.tinycraft.world.World
import kotlin.math.floor
import kotlin.math.sqrt

/** Applies movement/jump/gravity intent while keeping feet on solid voxel terrain. */
class PlayerMovementSystem(private val world: World) {
    fun update(delta: Float, player: PlayerState, inputState: InputState) {
        val safeDelta = delta.coerceIn(0f, 0.05f)
        applyHorizontalMovement(safeDelta, player, inputState)

        if (inputState.consumeAction(GameAction.JUMP) && player.grounded) {
            player.velocity.y = PlayerConfig.JUMP_SPEED
            player.grounded = false
        }

        if (!player.grounded || player.velocity.y > 0f) {
            player.velocity.y += PlayerConfig.GRAVITY * safeDelta
        }

        val groundY = groundFeetY(player.position.x, player.position.z)
        val nextY = player.position.y + player.velocity.y * safeDelta
        if (groundY != null && nextY <= groundY && player.velocity.y <= 0f) {
            player.position.y = groundY
            player.velocity.y = 0f
            player.grounded = true
        } else {
            player.position.y = nextY
            player.grounded = false
        }
    }

    private fun applyHorizontalMovement(delta: Float, player: PlayerState, inputState: InputState) {
        var side = inputState.moveX
        var forward = inputState.moveForward
        val lengthSquared = side * side + forward * forward
        if (lengthSquared > 1f) {
            val length = sqrt(lengthSquared)
            side /= length
            forward /= length
        }
        if (side == 0f && forward == 0f) return

        val yaw = player.yawDegrees * MathUtils.degreesToRadians
        val forwardX = -MathUtils.sin(yaw)
        val forwardZ = -MathUtils.cos(yaw)
        val rightX = MathUtils.cos(yaw)
        val rightZ = -MathUtils.sin(yaw)
        val speed = PlayerConfig.MOVE_SPEED_BLOCKS_PER_SECOND * delta
        val nextX = player.position.x + (rightX * side + forwardX * forward) * speed
        val nextZ = player.position.z + (rightZ * side + forwardZ * forward) * speed

        if (player.grounded) {
            val targetGround = groundFeetY(nextX, nextZ) ?: return
            if (targetGround > player.position.y + PlayerConfig.MAX_STEP_HEIGHT) return
        }

        player.position.x = nextX
        player.position.z = nextZ
    }

    private fun groundFeetY(x: Float, z: Float): Float? {
        val blockX = floor(x.toDouble()).toInt()
        val blockZ = floor(z.toDouble()).toInt()
        val surfaceY = world.findHighestSolidY(blockX, blockZ)
        return if (surfaceY >= 0) surfaceY + 1f else null
    }
}
