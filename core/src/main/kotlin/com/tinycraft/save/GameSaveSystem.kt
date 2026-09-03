package com.tinycraft.save

import com.tinycraft.blocks.BlockId
import com.tinycraft.config.WorldConfig
import com.tinycraft.inventory.ItemStack
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.player.PlayerState
import com.tinycraft.world.World

/** Captures, validates, persists, and restores versioned TinyCraft game state. */
class GameSaveSystem(
    private val repository: SaveRepository,
    private val codec: SaveCodec = SaveCodec()
) {
    fun loadCompatible(): SaveData? {
        val decoded = repository.load()?.let(codec::decode) ?: return null
        return decoded.takeIf(::isCompatible)
    }

    fun save(worldSeed: Long, world: World, player: PlayerState, inventory: PlayerInventoryState) {
        repository.save(codec.encode(capture(worldSeed, world, player, inventory)))
    }

    fun capture(
        worldSeed: Long,
        world: World,
        player: PlayerState,
        inventory: PlayerInventoryState
    ): SaveData {
        val slots = inventory.snapshot().mapIndexedNotNull { index, stack ->
            stack?.let { HotbarSlotSaveData(index, it.blockId.value, it.count) }
        }
        val edits = world.modifications().map { edit ->
            WorldModificationSaveData(edit.x, edit.y, edit.z, edit.blockId.value)
        }
        return SaveData(
            version = WorldConfig.SAVE_VERSION,
            generationVersion = WorldConfig.GENERATION_VERSION,
            worldSeed = worldSeed,
            player = PlayerSaveData(
                player.position.x,
                player.position.y,
                player.position.z,
                player.yawDegrees,
                player.pitchDegrees
            ),
            selectedHotbarSlot = inventory.selectedSlotIndex,
            hotbarSlots = slots,
            modifications = edits
        )
    }

    fun restore(data: SaveData, world: World, player: PlayerState, inventory: PlayerInventoryState) {
        require(isCompatible(data)) { "Incompatible TinyCraft save" }

        data.modifications.forEach { edit ->
            world.setBlock(edit.x, edit.y, edit.z, blockId(edit.blockIdValue))
        }

        player.position.set(data.player.x, data.player.y, data.player.z)
        player.velocity.setZero()
        player.yawDegrees = data.player.yawDegrees
        player.pitchDegrees = data.player.pitchDegrees
        player.grounded = false

        val restoredSlots = MutableList<ItemStack?>(PlayerInventoryState.HOTBAR_SIZE) { null }
        data.hotbarSlots.forEach { saved ->
            restoredSlots[saved.slot] = ItemStack(blockId(saved.blockIdValue), saved.count)
        }
        inventory.restore(data.selectedHotbarSlot, restoredSlots)
    }

    private fun isCompatible(data: SaveData): Boolean {
        if (data.version != WorldConfig.SAVE_VERSION) return false
        if (data.generationVersion != WorldConfig.GENERATION_VERSION) return false
        if (data.selectedHotbarSlot !in 0 until PlayerInventoryState.HOTBAR_SIZE) return false
        if (data.hotbarSlots.map { it.slot }.distinct().size != data.hotbarSlots.size) return false

        for (slot in data.hotbarSlots) {
            if (slot.slot !in 0 until PlayerInventoryState.HOTBAR_SIZE) return false
            val block = findBlockId(slot.blockIdValue) ?: return false
            if (block == BlockId.AIR || slot.count !in 1..ItemStack.MAX_STACK_SIZE) return false
        }
        for (edit in data.modifications) {
            if (edit.y !in 0 until WorldConfig.CHUNK_HEIGHT) return false
            if (findBlockId(edit.blockIdValue) == null) return false
        }
        return true
    }

    private fun blockId(value: Int): BlockId = requireNotNull(findBlockId(value)) {
        "Unknown block ID in compatible save: $value"
    }

    private fun findBlockId(value: Int): BlockId? = BlockId.entries.firstOrNull { it.value == value }
}
