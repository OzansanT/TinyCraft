package com.tinycraft.rendering

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.tinycraft.theme.GameColors
import com.tinycraft.world.World

/** Read-only presentation layer for World. Camera ownership stays with the player camera controller. */
class WorldRenderer(private val world: World) : Disposable {
    private val chunkRenderCache = ChunkRenderCache()
    private val shader = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER).also {
        require(it.isCompiled) { "TinyCraft voxel shader failed to compile: ${it.log}" }
    }

    fun render(camera: Camera) {
        val sky = GameColors.SKY
        Gdx.gl.glClearColor(sky.r, sky.g, sky.b, sky.a)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        chunkRenderCache.synchronize(world)

        shader.bind()
        shader.setUniformMatrix("u_projView", camera.combined)
        chunkRenderCache.meshes().forEach { mesh ->
            mesh.render(shader, GL20.GL_TRIANGLES)
        }
    }

    override fun dispose() {
        chunkRenderCache.dispose()
        shader.dispose()
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec3 a_position;
            attribute vec4 a_color;
            uniform mat4 u_projView;
            varying vec4 v_color;

            void main() {
                v_color = a_color;
                gl_Position = u_projView * vec4(a_position, 1.0);
            }
        """

        private const val FRAGMENT_SHADER = """
            #ifdef GL_ES
            precision mediump float;
            #endif
            varying vec4 v_color;

            void main() {
                gl_FragColor = v_color;
            }
        """
    }
}
