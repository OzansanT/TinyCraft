package com.tinycraft.save

/** Stable in-memory schema for TinyCraft save version 1. */
data class SaveData(
    val version: Int,
    val generationVersion: Int,
    val worldSeed: Long,
    val player: PlayerSaveData,
    val selectedHotbarSlot: Int,
    val hotbarSlots: List<HotbarSlotSaveData>,
    val modifications: List<WorldModificationSaveData>
)

data class PlayerSaveData(
    val x: Float,
    val y: Float,
    val z: Float,
    val yawDegrees: Float,
    val pitchDegrees: Float
)

data class HotbarSlotSaveData(
    val slot: Int,
    val blockIdValue: Int,
    val count: Int
)

data class WorldModificationSaveData(
    val x: Int,
    val y: Int,
    val z: Int,
    val blockIdValue: Int
)
