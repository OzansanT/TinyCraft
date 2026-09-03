package com.tinycraft.player

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.tinycraft.blocks.BlockId
import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerInteractionSystemTest {
    @Test
    fun miningCollectsAndPlacementConsumesSelectedStack() {
        val world = World()
        val player = PlayerState().apply { position.set(0.5f, 0f, 0.5f) }
        val inventory = PlayerInventoryState()
        val system = PlayerInteractionSystem(world, player, inventory)
        val input = InputState()
        val camera = PerspectiveCamera(67f, 100f, 100f).apply {
            position.set(0.5f, 1.5f, 0.5f)
            direction.set(0f, 0f, 1f)
            near = 0.1f
            far = 20f
            update()
        }

        world.setBlock(0, 1, 3, BlockId.STONE)
        input.queueAction(GameAction.MINE)
        system.update(input, camera)

        assertEquals(BlockId.AIR, world.getBlock(0, 1, 3))
        assertEquals(BlockId.STONE, inventory.selectedStack()?.blockId)
        assertEquals(1, inventory.selectedStack()?.count)

        world.setBlock(0, 1, 3, BlockId.DIRT)
        input.queueAction(GameAction.PLACE)
        system.update(input, camera)

        assertEquals(BlockId.STONE, world.getBlock(0, 1, 2))
        assertNull(inventory.selectedStack())
    }
}
