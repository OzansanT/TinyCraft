package com.tinycraft.player

import com.tinycraft.blocks.BlockId
import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertTrue

class PlayerMovementSystemTest {
    @Test
    fun forwardIntentMovesPlayerRelativeToYaw() {
        val world = flatWorld()
        val player = PlayerState()
        PlayerSpawnSystem(world).spawn(player, 0, 0)
        player.yawDegrees = 0f
        val startZ = player.position.z
        val input = InputState()
        input.setMovement(0f, 1f)

        PlayerMovementSystem(world).update(0.016f, player, input)

        assertTrue(player.position.z < startZ)
    }

    @Test
    fun jumpActionRaisesVerticalVelocityWhenGrounded() {
        val world = flatWorld()
        val player = PlayerState()
        PlayerSpawnSystem(world).spawn(player, 0, 0)
        val input = InputState()
        input.queueAction(GameAction.JUMP)

        PlayerMovementSystem(world).update(0.016f, player, input)

        assertTrue(player.velocity.y > 0f)
        assertTrue(!player.grounded)
    }

    private fun flatWorld(): World = World().also { world ->
        for (x in -1..1) {
            for (z in -1..1) {
                world.setBlock(x, 0, z, BlockId.STONE)
            }
        }
    }
}
