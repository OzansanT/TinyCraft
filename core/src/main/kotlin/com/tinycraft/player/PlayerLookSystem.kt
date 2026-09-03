package com.tinycraft.player

import com.badlogic.gdx.math.Vector2
import com.tinycraft.config.PlayerConfig
import com.tinycraft.input.InputState

/** Converts accumulated look intent into authoritative yaw/pitch. */
class PlayerLookSystem {
    private val lookDelta = Vector2()

    fun update(player: PlayerState, inputState: InputState) {
        inputState.consumeLookDelta(lookDelta)
        if (lookDelta.len2() == 0f) return

        player.yawDegrees -= lookDelta.x * PlayerConfig.LOOK_SENSITIVITY_DEGREES_PER_PIXEL
        player.pitchDegrees = (
            player.pitchDegrees - lookDelta.y * PlayerConfig.LOOK_SENSITIVITY_DEGREES_PER_PIXEL
        ).coerceIn(PlayerConfig.MIN_PITCH_DEGREES, PlayerConfig.MAX_PITCH_DEGREES)

        if (player.yawDegrees > 180f) player.yawDegrees -= 360f
        if (player.yawDegrees < -180f) player.yawDegrees += 360f
    }
}
