package com.tinycraft.save

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SaveCodecTest {
    private val codec = SaveCodec()

    @Test
    fun roundTripPreservesSchema() {
        val original = SaveData(
            version = 1,
            generationVersion = 1,
            worldSeed = 783429L,
            player = PlayerSaveData(1.5f, 9f, -3.25f, 45f, -10f),
            selectedHotbarSlot = 2,
            hotbarSlots = listOf(
                HotbarSlotSaveData(0, 2, 64),
                HotbarSlotSaveData(2, 3, 7)
            ),
            modifications = listOf(
                WorldModificationSaveData(4, 8, 6, 0),
                WorldModificationSaveData(-2, 7, 5, 3)
            )
        )

        assertEquals(original, codec.decode(codec.encode(original)))
    }

    @Test
    fun invalidHeaderIsRejected() {
        assertNull(codec.decode("not-a-tinycraft-save\nversion=1"))
    }
}
