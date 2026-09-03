package com.tinycraft.input

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.utils.Disposable

/** Composes input components without embedding gameplay behavior in them. */
class GameInputController(inputState: InputState) : Disposable {
    private val multiplexer = InputMultiplexer(
        JumpButton(inputState),
        MineButton(inputState),
        PlaceButton(inputState),
        VirtualJoystickController(inputState),
        TouchLookController(inputState)
    )
    private var previousProcessor: InputProcessor? = null

    fun activate() {
        previousProcessor = Gdx.input.inputProcessor
        Gdx.input.inputProcessor = multiplexer
    }

    fun deactivate() {
        if (Gdx.input.inputProcessor === multiplexer) {
            Gdx.input.inputProcessor = previousProcessor
        }
        previousProcessor = null
    }

    override fun dispose() {
        deactivate()
    }
}
