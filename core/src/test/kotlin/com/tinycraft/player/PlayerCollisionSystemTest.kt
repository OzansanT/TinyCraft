package com.tinycraft.player

import com.tinycraft.blocks.BlockId
import com.tinycraft.world.ChunkPosition
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PlayerCollisionSystemTest {
    @Test
    fun horizontalMovementStopsAtSolidWall() {
        val world = World()
        world.getOrCreateChunk(ChunkPosition(0, 0))
        world.setBlock(2, 1, 1, BlockId.STONE)
        world.setBlock(2, 2, 1, BlockId.STONE)

        val player = PlayerState().apply {
            position.set(1.5f, 1f, 1.5f)
            grounded = false
        }
        val collision = PlayerCollisionSystem(world)

        collision.moveHorizontal(player, 1f, 0f)

        assertTrue(player.position.x < 1.71f)
    }

    @Test
    fun fallingPlayerLandsOnBlockTop() {
        val world = World()
        world.getOrCreateChunk(ChunkPosition(0, 0))
        world.setBlock(0, 0, 0, BlockId.STONE)
        val player = PlayerState().apply {
            position.set(0.5f, 2f, 0.5f)
            velocity.y = -8f
            grounded = false
        }
        val collision = PlayerCollisionSystem(world)

        collision.moveVertical(player, -2f)

        assertTrue(player.grounded)
        assertEquals(1f, player.position.y, 0.01f)
        assertEquals(0f, player.velocity.y)
    }
}
