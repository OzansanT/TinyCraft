package com.tinycraft.nativeandroid

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * GLES 2.0 compatibility renderer used after the first native build showed a
 * black surface on some Android GPU drivers. It deliberately avoids GLSL
 * constructors and state that are known to vary across mobile drivers.
 */
class SafeVoxelRenderer(private val ui: GameUiListener) : GLSurfaceView.Renderer {
    private val world = VoxelWorld()

    var yaw = (Math.PI * 0.25).toFloat()
        private set
    private var pitch = 0.55f
    private var distance = 8f

    private var viewportWidth = 1
    private var viewportHeight = 1
    private var ready = false

    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val inverseVp = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)

    private var program = 0
    private var aPosition = -1
    private var aNormal = -1
    private var uMvp = -1
    private var uColor = -1
    private var uLightDir = -1
    private var uFogColor = -1
    private var uFogNear = -1
    private var uFogFar = -1

    private lateinit var cubeBuffer: FloatBuffer
    private var vertexCount = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(SKY_R, SKY_G, SKY_B, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glDisable(GLES20.GL_CULL_FACE)

        try {
            cubeBuffer = buildCubeBuffer()
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            aPosition = GLES20.glGetAttribLocation(program, "aPosition")
            aNormal = GLES20.glGetAttribLocation(program, "aNormal")
            uMvp = GLES20.glGetUniformLocation(program, "uMvp")
            uColor = GLES20.glGetUniformLocation(program, "uColor")
            uLightDir = GLES20.glGetUniformLocation(program, "uLightDir")
            uFogColor = GLES20.glGetUniformLocation(program, "uFogColor")
            uFogNear = GLES20.glGetUniformLocation(program, "uFogNear")
            uFogFar = GLES20.glGetUniformLocation(program, "uFogFar")

            if (program == 0 || aPosition < 0 || aNormal < 0 || uMvp < 0 || uColor < 0) {
                error("required GLES 2.0 shader locations unavailable")
            }
            ready = true
            emitHud()
        } catch (t: Throwable) {
            ready = false
            program = 0
            ui.onMessage("Graphics initialization failed: ${t.message ?: "OpenGL ES 2.0 error"}")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        Matrix.perspectiveM(
            projection,
            0,
            62f,
            viewportWidth.toFloat() / viewportHeight.toFloat(),
            0.1f,
            100f
        )
        updateCameraMatrices()
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(SKY_R, SKY_G, SKY_B, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (!ready || program == 0) return

        updateCameraMatrices()
        GLES20.glUseProgram(program)

        cubeBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(
            aPosition,
            3,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            cubeBuffer
        )
        cubeBuffer.position(3)
        GLES20.glEnableVertexAttribArray(aNormal)
        GLES20.glVertexAttribPointer(
            aNormal,
            3,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            cubeBuffer
        )

        if (uLightDir >= 0) GLES20.glUniform3f(uLightDir, 8f, 16f, 5f)
        if (uFogColor >= 0) GLES20.glUniform3f(uFogColor, SKY_R, SKY_G, SKY_B)
        if (uFogNear >= 0) GLES20.glUniform1f(uFogNear, 14f)
        if (uFogFar >= 0) GLES20.glUniform1f(uFogFar, 34f)

        world.allBlocks.forEach { block ->
            drawCube(
                block.x.toFloat(),
                block.y.toFloat(),
                block.z.toFloat(),
                1f,
                1f,
                1f,
                block.type.color
            )
        }

        drawCube(
            world.playerX,
            world.playerY(),
            world.playerZ,
            0.55f,
            1.15f,
            0.55f,
            PLAYER_COLOR
        )

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aNormal)
    }

    fun resetWorld() {
        world.generateWorld()
        emitHud()
        ui.onMessage("New world generated. Click to mine; Shift + click to place.")
    }

    fun selectBlock(type: BlockType) {
        world.select(type)
        emitHud()
        ui.onMessage("${type.label} block selected.")
    }

    fun rotate(dx: Float, dy: Float) {
        yaw -= dx * 0.008f
        pitch = (pitch + dy * 0.006f).coerceIn(0.2f, 1.25f)
    }

    fun zoom(scaleFactor: Float) {
        if (scaleFactor > 0f && scaleFactor.isFinite()) {
            distance = (distance / scaleFactor).coerceIn(4.5f, 13f)
        }
    }

    fun move(forward: Float, strafe: Float) {
        world.move(forward, strafe, yaw)
        emitHud()
    }

    fun interact(screenX: Float, screenY: Float, place: Boolean) {
        if (!ready) {
            ui.onMessage("Graphics renderer is not ready.")
            return
        }
        val pick = pick(screenX, screenY)
        if (pick == null) {
            ui.onMessage("No block selected. Aim at the terrain.")
            return
        }

        if (place) {
            val placed = world.place(pick.placeX, pick.placeY, pick.placeZ)
            ui.onMessage(
                if (placed) "${world.selectedBlock.label.lowercase()} block placed."
                else "That space is occupied."
            )
        } else {
            if (pick.hit.y == 0) {
                ui.onMessage("The bottom foundation cannot be mined.")
                return
            }
            world.mine(pick.hit)
            ui.onMessage("Block mined.")
        }
        emitHud()
    }

    private fun drawCube(
        x: Float,
        y: Float,
        z: Float,
        sx: Float,
        sy: Float,
        sz: Float,
        color: FloatArray
    ) {
        Matrix.setIdentityM(model, 0)
        Matrix.translateM(model, 0, x, y, z)
        Matrix.scaleM(model, 0, sx, sy, sz)
        Matrix.multiplyMM(mvp, 0, vp, 0, model, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvp, 0)
        GLES20.glUniform4fv(uColor, 1, color, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
    }

    private fun updateCameraMatrices() {
        val targetX = world.playerX
        val targetY = world.playerY() + 0.5f
        val targetZ = world.playerZ
        val horizontal = cos(pitch) * distance
        val cameraX = targetX + sin(yaw) * horizontal
        val cameraY = targetY + sin(pitch) * distance
        val cameraZ = targetZ + cos(yaw) * horizontal

        Matrix.setLookAtM(
            view,
            0,
            cameraX,
            cameraY,
            cameraZ,
            targetX,
            targetY,
            targetZ,
            0f,
            1f,
            0f
        )
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)
        Matrix.invertM(inverseVp, 0, vp, 0)
    }

    private fun pick(screenX: Float, screenY: Float): PickResult? {
        updateCameraMatrices()
        val nx = 2f * screenX / viewportWidth.toFloat() - 1f
        val ny = 1f - 2f * screenY / viewportHeight.toFloat()
        val near = unproject(nx, ny, -1f)
        val far = unproject(nx, ny, 1f)

        var dx = far[0] - near[0]
        var dy = far[1] - near[1]
        var dz = far[2] - near[2]
        val length = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.0001f)
        dx /= length
        dy /= length
        dz /= length

        var previousX = near[0].roundToInt()
        var previousY = near[1].roundToInt()
        var previousZ = near[2].roundToInt()
        var t = 0f
        while (t <= 28f) {
            val bx = (near[0] + dx * t).roundToInt()
            val by = (near[1] + dy * t).roundToInt()
            val bz = (near[2] + dz * t).roundToInt()
            val block = world.blockAt(bx, by, bz)
            if (block != null) return PickResult(block, previousX, previousY, previousZ)
            previousX = bx
            previousY = by
            previousZ = bz
            t += 0.045f
        }
        return null
    }

    private fun unproject(x: Float, y: Float, z: Float): FloatArray {
        val input = floatArrayOf(x, y, z, 1f)
        val output = FloatArray(4)
        Matrix.multiplyMV(output, 0, inverseVp, 0, input, 0)
        val w = if (abs(output[3]) < 0.0001f) 1f else output[3]
        return floatArrayOf(output[0] / w, output[1] / w, output[2] / w)
    }

    private fun emitHud() {
        ui.onHud(
            world.selectedBlock,
            world.blockCount,
            world.playerX.roundToInt(),
            world.playerZ.roundToInt()
        )
    }

    private fun buildCubeBuffer(): FloatBuffer {
        val faces = arrayOf(
            face(0f, 0f, 1f, -1f,-1f,1f, 1f,-1f,1f, 1f,1f,1f, -1f,1f,1f),
            face(0f, 0f,-1f, 1f,-1f,-1f, -1f,-1f,-1f, -1f,1f,-1f, 1f,1f,-1f),
            face(1f, 0f, 0f, 1f,-1f,1f, 1f,-1f,-1f, 1f,1f,-1f, 1f,1f,1f),
            face(-1f,0f, 0f, -1f,-1f,-1f, -1f,-1f,1f, -1f,1f,1f, -1f,1f,-1f),
            face(0f, 1f, 0f, -1f,1f,1f, 1f,1f,1f, 1f,1f,-1f, -1f,1f,-1f),
            face(0f,-1f, 0f, -1f,-1f,-1f, 1f,-1f,-1f, 1f,-1f,1f, -1f,-1f,1f)
        )
        val data = ArrayList<Float>(36 * 6)
        val order = intArrayOf(0, 1, 2, 0, 2, 3)
        faces.forEach { f ->
            order.forEach { index ->
                val p = f.points[index]
                data += p[0] * 0.5f
                data += p[1] * 0.5f
                data += p[2] * 0.5f
                data += f.nx
                data += f.ny
                data += f.nz
            }
        }
        vertexCount = data.size / 6
        return ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                data.forEach { put(it) }
                position(0)
            }
    }

    private fun face(
        nx: Float, ny: Float, nz: Float,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float
    ) = Face(
        nx, ny, nz,
        arrayOf(
            floatArrayOf(x0, y0, z0),
            floatArrayOf(x1, y1, z1),
            floatArrayOf(x2, y2, z2),
            floatArrayOf(x3, y3, z3)
        )
    )

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val p = GLES20.glCreateProgram()
        if (p == 0) error("glCreateProgram returned 0")
        GLES20.glAttachShader(p, vertexShader)
        GLES20.glAttachShader(p, fragmentShader)
        GLES20.glLinkProgram(p)
        val status = IntArray(1)
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, status, 0)
        val log = GLES20.glGetProgramInfoLog(p)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        if (status[0] == 0) {
            GLES20.glDeleteProgram(p)
            error("program link failed: $log")
        }
        return p
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) error("glCreateShader returned 0")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("shader compile failed: $log")
        }
        return shader
    }

    private data class Face(
        val nx: Float,
        val ny: Float,
        val nz: Float,
        val points: Array<FloatArray>
    )

    companion object {
        private const val STRIDE_BYTES = 6 * 4
        private const val SKY_R = 0.561f
        private const val SKY_G = 0.780f
        private const val SKY_B = 0.910f
        private val PLAYER_COLOR = floatArrayOf(0.949f, 0.788f, 0.298f, 1f)

        // GLSL ES 1.00 only. In particular, do not use mat3(mat4), which caused
        // the original renderer to fail on a subset of Android GLES drivers.
        private const val VERTEX_SHADER = """
            uniform mat4 uMvp;
            attribute vec3 aPosition;
            attribute vec3 aNormal;
            varying vec3 vNormal;
            varying float vDistance;
            void main() {
                vec4 clipPos = uMvp * vec4(aPosition, 1.0);
                gl_Position = clipPos;
                vNormal = aNormal;
                vDistance = abs(clipPos.w);
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 uColor;
            uniform vec3 uLightDir;
            uniform vec3 uFogColor;
            uniform float uFogNear;
            uniform float uFogFar;
            varying vec3 vNormal;
            varying float vDistance;
            void main() {
                float diffuse = max(dot(normalize(vNormal), normalize(uLightDir)), 0.0);
                float lighting = 0.58 + diffuse * 0.62;
                vec3 lit = uColor.rgb * lighting;
                float fogAmount = clamp((vDistance - uFogNear) / (uFogFar - uFogNear), 0.0, 1.0);
                gl_FragColor = vec4(mix(lit, uFogColor, fogAmount), uColor.a);
            }
        """
    }
}
