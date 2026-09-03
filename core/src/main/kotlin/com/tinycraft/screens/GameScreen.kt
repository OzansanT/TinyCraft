package com.tinycraft.screens

import com.badlogic.gdx.ScreenAdapter
import com.tinycraft.config.WorldConfig
import com.tinycraft.rendering.WorldRenderer
import com.tinycraft.world.World
import com.tinycraft.world.generation.WorldGenerator

/** Composes game state and rendering without owning domain behavior itself. */
class GameScreen : ScreenAdapter() {
    private val world = World().also { generatedWorld ->
        WorldGenerator(WorldConfig.DEFAULT_WORLD_SEED).generateInitialWorld(generatedWorld)
    }
    private val worldRenderer = WorldRenderer(world)

    override fun render(delta: Float) {
        worldRenderer.render(delta)
    }

    override fun resize(width: Int, height: Int) {
        worldRenderer.resize(width, height)
    }

    override fun dispose() {
        worldRenderer.dispose()
    }
}
