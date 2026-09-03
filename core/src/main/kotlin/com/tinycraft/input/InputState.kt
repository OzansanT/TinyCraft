package com.tinycraft.input

import com.badlogic.gdx.math.Vector2
import java.util.EnumSet

/** Shared platform-neutral input state. Controllers write intents; gameplay systems consume them. */
class InputState {
    var moveX: Float = 0f
        private set
    var moveForward: Float = 0f
        private set

    private var lookDeltaX = 0f
    private var lookDeltaY = 0f
    private var pendingHotbarSlot = -1
    private val queuedActions = EnumSet.noneOf(GameAction::class.java)

    fun setMovement(x: Float, forward: Float) {
        moveX = x.coerceIn(-1f, 1f)
        moveForward = forward.coerceIn(-1f, 1f)
    }

    fun addLookDelta(deltaX: Float, deltaY: Float) {
        lookDeltaX += deltaX
        lookDeltaY += deltaY
    }

    fun consumeLookDelta(out: Vector2): Vector2 {
        out.set(lookDeltaX, lookDeltaY)
        lookDeltaX = 0f
        lookDeltaY = 0f
        return out
    }

    fun queueAction(action: GameAction) {
        queuedActions.add(action)
    }

    fun consumeAction(action: GameAction): Boolean = queuedActions.remove(action)

    fun queueHotbarSelection(slot: Int) {
        pendingHotbarSlot = slot
        queuedActions.add(GameAction.SELECT_HOTBAR_SLOT)
    }

    fun consumeHotbarSelection(): Int? {
        if (!queuedActions.remove(GameAction.SELECT_HOTBAR_SLOT)) return null
        val slot = pendingHotbarSlot
        pendingHotbarSlot = -1
        return slot.takeIf { it >= 0 }
    }

    fun reset() {
        moveX = 0f
        moveForward = 0f
        lookDeltaX = 0f
        lookDeltaY = 0f
        pendingHotbarSlot = -1
        queuedActions.clear()
    }
}
