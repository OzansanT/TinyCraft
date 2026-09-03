package com.tinycraft.session

/** Owns game-session state that is neither world data nor UI state. */
class GameSessionState {
    var paused: Boolean = false
        internal set
}
