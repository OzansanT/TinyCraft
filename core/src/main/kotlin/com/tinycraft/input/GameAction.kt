package com.tinycraft.input

/** Platform-neutral intents consumed by gameplay systems. */
enum class GameAction {
    MOVE_FORWARD,
    MOVE_BACKWARD,
    MOVE_LEFT,
    MOVE_RIGHT,
    LOOK,
    JUMP,
    MINE,
    PLACE,
    SELECT_HOTBAR_SLOT,
    OPEN_INVENTORY,
    PAUSE
}
