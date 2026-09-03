package com.tinycraft.player

import com.tinycraft.config.PlayerConfig
import com.tinycraft.input.InputState
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerLookSystemTest {
    @Test
    fun lookIntentChangesYawAndPitch() {
        val player = PlayerState()
        player.yawDegrees = 0f
        player.pitchDegrees = 0f
        val input = InputState()
        input.addLookDelta(10f, -5f)

        PlayerLookSystem().update(player, input)

        assertEquals(-10f * PlayerConfig.LOOK_SENSITIVITY_DEGREES_PER_PIXEL, player.yawDegrees, 0.0001f)
        assertEquals(5f * PlayerConfig.LOOK_SENSITIVITY_DEGREES_PER_PIXEL, player.pitchDegrees, 0.0001f)
    }

    @Test
    fun pitchIsClamped() {
        val player = PlayerState()
        val input = InputState()
        input.addLookDelta(0f, 10000f)

        PlayerLookSystem().update(player, input)

        assertEquals(PlayerConfig.MIN_PITCH_DEGREES, player.pitchDegrees)
    }
}
