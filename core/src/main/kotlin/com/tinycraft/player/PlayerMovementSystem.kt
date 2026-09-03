package com.tinycraft.player

import com.badlogic.gdx.math.MathUtils
import com.tinycraft.config.PlayerConfig
import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState
import com.tinycraft.world.World
import kotlin.math.sqrt

/** Converts movement intent into velocity and delegates voxel contact resolution to PlayerCollisionSystem. */
class PlayerMovementSystem(world: World) {
    private val collisionSystem = PlayerCollisionSystem(world)

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

        collisionSystem.moveVertical(player, player.velocity.y * safeDelta)
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
        val deltaX = (rightX * side + forwardX * forward) * speed
        val deltaZ = (rightZ * side + forwardZ * forward) * speed

        collisionSystem.moveHorizontal(player, deltaX, deltaZ)
    }
}
