package com.tinycraft.screens

import com.badlogic.gdx.ScreenAdapter
import com.tinycraft.rendering.WorldRenderer
import com.tinycraft.world.World

/** Composes game state and rendering without owning domain behavior itself. */
class GameScreen : ScreenAdapter() {
    private val world = World()
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
