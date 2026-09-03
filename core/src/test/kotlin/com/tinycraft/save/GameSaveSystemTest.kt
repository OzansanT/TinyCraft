package com.tinycraft.save

import com.tinycraft.blocks.BlockId
import com.tinycraft.config.WorldConfig
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.player.PlayerState
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GameSaveSystemTest {
    @Test
    fun saveAndRestoreRoundTrip() {
        val repository = MemorySaveRepository()
        val system = GameSaveSystem(repository)
        val world = World()
        val player = PlayerState().apply {
            position.set(3.5f, 11f, -2.5f)
            yawDegrees = 90f
            pitchDegrees = -20f
        }
        val inventory = PlayerInventoryState().apply {
            add(BlockId.STONE, 9)
            selectSlot(0)
        }
        world.setBlock(2, 8, 2, BlockId.STONE)

        system.save(WorldConfig.DEFAULT_WORLD_SEED, world, player, inventory)
        val loaded = assertNotNull(system.loadCompatible())

        val restoredWorld = World()
        val restoredPlayer = PlayerState()
        val restoredInventory = PlayerInventoryState()
        system.restore(loaded, restoredWorld, restoredPlayer, restoredInventory)

        assertEquals(BlockId.STONE, restoredWorld.getBlock(2, 8, 2))
        assertEquals(player.position, restoredPlayer.position)
        assertEquals(90f, restoredPlayer.yawDegrees)
        assertEquals(BlockId.STONE, restoredInventory.selectedStack()?.blockId)
        assertEquals(9, restoredInventory.selectedStack()?.count)
    }

    private class MemorySaveRepository : SaveRepository {
        private var content: String? = null
        override fun load(): String? = content
        override fun save(content: String) {
            this.content = content
        }
    }
}
