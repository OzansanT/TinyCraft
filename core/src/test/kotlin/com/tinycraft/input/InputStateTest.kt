package com.tinycraft.input

import com.badlogic.gdx.math.Vector2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputStateTest {
    @Test
    fun actionsAreConsumedOnce() {
        val input = InputState()
        input.queueAction(GameAction.JUMP)

        assertTrue(input.consumeAction(GameAction.JUMP))
        assertFalse(input.consumeAction(GameAction.JUMP))
    }

    @Test
    fun lookDeltaIsAccumulatedAndClearedOnConsume() {
        val input = InputState()
        val out = Vector2()
        input.addLookDelta(3f, -2f)
        input.addLookDelta(1f, 1f)

        input.consumeLookDelta(out)
        assertEquals(4f, out.x)
        assertEquals(-1f, out.y)

        input.consumeLookDelta(out)
        assertEquals(0f, out.x)
        assertEquals(0f, out.y)
    }
}
