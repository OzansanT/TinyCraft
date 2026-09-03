package com.tinycraft.player

import com.badlogic.gdx.math.Vector3

/** Authoritative player-owned pose and motion state. Position represents the player's feet. */
class PlayerState {
    val position = Vector3(0f, 4f, 0f)
    val velocity = Vector3.Zero.cpy()

    var yawDegrees: Float = 0f
        internal set

    var pitchDegrees: Float = -12f
        internal set

    var grounded: Boolean = false
        internal set
}
