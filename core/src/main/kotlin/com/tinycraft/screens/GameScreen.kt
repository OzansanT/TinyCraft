package com.tinycraft.screens

import com.badlogic.gdx.ScreenAdapter
import com.tinycraft.config.WorldConfig
import com.tinycraft.input.GameInputController
import com.tinycraft.input.InputState
import com.tinycraft.inventory.HotbarSelectionSystem
import com.tinycraft.player.PlayerCameraController
import com.tinycraft.player.PlayerInteractionSystem
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.player.PlayerLookSystem
import com.tinycraft.player.PlayerMovementSystem
import com.tinycraft.player.PlayerSpawnSystem
import com.tinycraft.player.PlayerState
import com.tinycraft.rendering.WorldRenderer
import com.tinycraft.save.GameSaveSystem
import com.tinycraft.save.LocalSaveRepository
import com.tinycraft.session.GameSessionState
import com.tinycraft.session.PauseSystem
import com.tinycraft.ui.TouchHudRenderer
import com.tinycraft.ui.hotbar.HotbarRenderer
import com.tinycraft.world.World
import com.tinycraft.world.generation.WorldGenerator
import com.tinycraft.world.streaming.ChunkStreamingSystem

/** Composes systems and presentation without absorbing their responsibilities. */
class GameScreen : ScreenAdapter() {
    private val saveSystem = GameSaveSystem(LocalSaveRepository())
    private val loadedSave = saveSystem.loadCompatible()
    private val worldSeed = loadedSave?.worldSeed ?: WorldConfig.DEFAULT_WORLD_SEED

    private val world = World()
    private val worldGenerator = WorldGenerator(worldSeed)
    private val chunkStreamingSystem = ChunkStreamingSystem(world, worldGenerator)

    private val inputState = InputState()
    private val player = PlayerState()
    private val inventory = PlayerInventoryState()
    private val session = GameSessionState()

    private val movementSystem = PlayerMovementSystem(world)
    private val lookSystem = PlayerLookSystem()
    private val hotbarSelectionSystem = HotbarSelectionSystem()
    private val interactionSystem = PlayerInteractionSystem(world, player, inventory)
    private val pauseSystem = PauseSystem()
    private val cameraController = PlayerCameraController(player)

    private val worldRenderer = WorldRenderer(world)
    private val touchHudRenderer = TouchHudRenderer(inputState)
    private val hotbarRenderer = HotbarRenderer(inventory)
    private val inputController = GameInputController(inputState)

    init {
        if (loadedSave != null) {
            saveSystem.restore(loadedSave, world, player, inventory)
            chunkStreamingSystem.update(player.position.x, player.position.z)
        } else {
            chunkStreamingSystem.update(8.5f, 8.5f)
            PlayerSpawnSystem(world).spawn(player, 8, 8)
        }
        cameraController.update()
    }

    override fun show() {
        inputController.activate()
    }

    override fun render(delta: Float) {
        val pauseChanged = pauseSystem.update(inputState, session)
        if (pauseChanged && session.paused) saveGame()

        if (!session.paused) {
            hotbarSelectionSystem.update(inputState, inventory)
            lookSystem.update(player, inputState)
            movementSystem.update(delta, player, inputState)
            chunkStreamingSystem.update(player.position.x, player.position.z)
            cameraController.update()
            interactionSystem.update(inputState, cameraController.camera)
        } else {
            cameraController.update()
        }

        worldRenderer.render(cameraController.camera)
        touchHudRenderer.render(session.paused)
        hotbarRenderer.render()
    }

    override fun resize(width: Int, height: Int) {
        cameraController.resize(width, height)
    }

    override fun hide() {
        saveGame()
        inputController.deactivate()
    }

    override fun dispose() {
        saveGame()
        inputController.dispose()
        hotbarRenderer.dispose()
        touchHudRenderer.dispose()
        worldRenderer.dispose()
    }

    private fun saveGame() {
        saveSystem.save(worldSeed, world, player, inventory)
    }
}
