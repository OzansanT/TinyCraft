package com.tinycraft.player

import com.badlogic.gdx.graphics.Camera
import com.tinycraft.blocks.BlockId
import com.tinycraft.blocks.BlockRegistry
import com.tinycraft.config.PlayerConfig
import com.tinycraft.config.WorldConfig
import com.tinycraft.input.GameAction
import com.tinycraft.input.InputState
import com.tinycraft.world.World

/** Performs inventory-aware mine/place mutations after resolving a player raycast target. */
class PlayerInteractionSystem(
    private val world: World,
    private val player: PlayerState,
    private val inventory: PlayerInventoryState
) {
    private val raycaster = VoxelRaycaster(world)

    fun update(inputState: InputState, camera: Camera) {
        val mine = inputState.consumeAction(GameAction.MINE)
        val place = inputState.consumeAction(GameAction.PLACE)
        if (!mine && !place) return

        val target = raycaster.raycast(camera.position, camera.direction) ?: return
        if (mine) mine(target)
        if (place) place(target)
    }

    private fun mine(target: BlockTarget) {
        if (target.blockY <= 0) return
        val blockId = world.getBlock(target.blockX, target.blockY, target.blockZ)
        if (blockId == BlockId.AIR || !BlockRegistry.get(blockId).solid) return
        if (!inventory.addOne(blockId)) return

        world.setBlock(target.blockX, target.blockY, target.blockZ, BlockId.AIR)
    }

    private fun place(target: BlockTarget) {
        if (target.adjacentY !in 0 until WorldConfig.CHUNK_HEIGHT) return
        if (world.getBlock(target.adjacentX, target.adjacentY, target.adjacentZ) != BlockId.AIR) return
        if (intersectsPlayer(target.adjacentX, target.adjacentY, target.adjacentZ)) return

        val stack = inventory.selectedStack() ?: return
        world.setBlock(target.adjacentX, target.adjacentY, target.adjacentZ, stack.blockId)
        inventory.consumeSelected()
    }

    private fun intersectsPlayer(blockX: Int, blockY: Int, blockZ: Int): Boolean {
        val overlapsX = blockX + 1f > player.position.x - PlayerConfig.PLAYER_RADIUS &&
            blockX < player.position.x + PlayerConfig.PLAYER_RADIUS
        val overlapsZ = blockZ + 1f > player.position.z - PlayerConfig.PLAYER_RADIUS &&
            blockZ < player.position.z + PlayerConfig.PLAYER_RADIUS
        val overlapsY = blockY + 1f > player.position.y &&
            blockY < player.position.y + PlayerConfig.PLAYER_HEIGHT
        return overlapsX && overlapsY && overlapsZ
    }
}
