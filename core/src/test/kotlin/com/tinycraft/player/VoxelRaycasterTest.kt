package com.tinycraft.player

import com.badlogic.gdx.math.Vector3
import com.tinycraft.blocks.BlockId
import com.tinycraft.world.World
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class VoxelRaycasterTest {
    @Test
    fun returnsHitAndPreviousEmptyVoxel() {
        val world = World()
        world.setBlock(0, 1, -3, BlockId.STONE)
        val target = VoxelRaycaster(world).raycast(
            Vector3(0.5f, 1.5f, 0.5f),
            Vector3(0f, 0f, -1f),
            5f
        )

        assertNotNull(target)
        assertEquals(0, target.blockX)
        assertEquals(1, target.blockY)
        assertEquals(-3, target.blockZ)
        assertEquals(0, target.adjacentX)
        assertEquals(1, target.adjacentY)
        assertEquals(-2, target.adjacentZ)
        assertEquals(BlockId.STONE, target.blockId)
    }
}
