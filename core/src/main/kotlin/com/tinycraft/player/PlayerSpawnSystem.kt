package com.tinycraft.player

import com.tinycraft.world.World

/** Places a player on the highest solid terrain at a requested horizontal coordinate. */
class PlayerSpawnSystem(private val world: World) {
    fun spawn(player: PlayerState, worldX: Int, worldZ: Int) {
        val surfaceY = world.findHighestSolidY(worldX, worldZ)
        player.position.set(worldX + 0.5f, surfaceY.coerceAtLeast(0) + 1f, worldZ + 0.5f)
        player.velocity.setZero()
        player.grounded = surfaceY >= 0
    }
}
