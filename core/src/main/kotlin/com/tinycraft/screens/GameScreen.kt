package com.tinycraft.screens

import com.badlogic.gdx.ScreenAdapter
import com.tinycraft.config.WorldConfig
import com.tinycraft.input.GameInputController
import com.tinycraft.input.InputState
import com.tinycraft.player.PlayerCameraController
import com.tinycraft.player.PlayerInteractionSystem
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.player.PlayerLookSystem
import com.tinycraft.player.PlayerMovementSystem
import com.tinycraft.player.PlayerSpawnSystem
import com.tinycraft.player.PlayerState
import com.tinycraft.rendering.WorldRenderer
import com.tinycraft.ui.TouchHudRenderer
import com.tinycraft.world.World
import com.tinycraft.world.generation.WorldGenerator

/** Composes systems and presentation without absorbing their responsibilities. */
class GameScreen : ScreenAdapter() {
    private val world = World().also { generatedWorld ->
        WorldGenerator(WorldConfig.DEFAULT_WORLD_SEED).generateInitialWorld(generatedWorld)
    }

    private val inputState = InputState()
    private val player = PlayerState().also { state -> PlayerSpawnSystem(world).spawn(state, 8, 8) }
    private val inventory = PlayerInventoryState()

    private val movementSystem = PlayerMovementSystem(world)
    private val lookSystem = PlayerLookSystem()
    private val interactionSystem = PlayerInteractionSystem(world, player, inventory)
    private val cameraController = PlayerCameraController(player)

    private val worldRenderer = WorldRenderer(world)
    private val touchHudRenderer = TouchHudRenderer(inputState)
    private val inputController = GameInputController(inputState)

    init {
        cameraController.update()
    }

    override fun show() {
        inputController.activate()
    }

    override fun render(delta: Float) {
        lookSystem.update(player, inputState)
        movementSystem.update(delta, player, inputState)
        cameraController.update()
        interactionSystem.update(inputState, cameraController.camera)

        worldRenderer.render(cameraController.camera)
        touchHudRenderer.render()
    }

    override fun resize(width: Int, height: Int) {
        cameraController.resize(width, height)
    }

    override fun hide() {
        inputController.deactivate()
    }

    override fun dispose() {
        inputController.dispose()
        touchHudRenderer.dispose()
        worldRenderer.dispose()
    }
}
