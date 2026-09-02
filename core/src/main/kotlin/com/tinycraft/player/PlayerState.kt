package com.tinycraft.player

import com.badlogic.gdx.math.Vector3

/** Authoritative player-owned state. UI should observe/act through systems rather than duplicate it. */
class PlayerState {
    val position = Vector3(0f, 4f, 0f)
    val velocity = Vector3.Zero.cpy()

    var grounded: Boolean = false
        internal set
}
