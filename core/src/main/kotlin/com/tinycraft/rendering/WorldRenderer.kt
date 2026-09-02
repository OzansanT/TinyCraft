package com.tinycraft.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Disposable
import com.tinycraft.theme.GameColors
import com.tinycraft.world.World

/**
 * Read-only presentation layer for World.
 * Chunk meshing and camera rendering will be introduced in the next milestone.
 */
class WorldRenderer(private val world: World) : Disposable {
    fun render(delta: Float) {
        val sky = GameColors.SKY
        Gdx.gl.glClearColor(sky.r, sky.g, sky.b, sky.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Intentionally no world mutation here. Future rendering reads world.loadedChunks().
        @Suppress("UNUSED_VARIABLE")
        val loadedChunkCount = world.loadedChunks().size
    }

    fun resize(width: Int, height: Int) {
        // Camera/viewport ownership is added with the chunk renderer.
    }

    override fun dispose() {
        // Dispose GPU resources here as render resources are introduced.
    }
}
