package com.tinycraft.nativeandroid

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLExt
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.Surface
import android.view.TextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Native Android GPU voxel view.
 *
 * TextureView keeps the game in the normal Android compositor while a dedicated
 * EGL thread renders chunk VBOs. OpenGL ES 3 is requested first; ES 2 is a
 * compatibility fallback using the same compact shader/VBO path.
 */
class GpuGameView(
    context: Context,
    private val listener: GameUiListener,
    private val onGpuFailure: (Throwable) -> Unit
) : TextureView(context), TextureView.SurfaceTextureListener {

    private val world = VoxelWorld()
    private val worldLock = Any()

    @Volatile private var yaw = (Math.PI * 0.25).toFloat()
    @Volatile private var pitch = 0.55f
    @Volatile private var distance = 8f

    private var renderThread: RenderThread? = null
    private var twoFingerMode = false
    private var lastCentroidX = 0f
    private var lastCentroidY = 0f

    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector

    init {
        surfaceTextureListener = this
        isOpaque = true
        isFocusable = true
        isFocusableInTouchMode = true

        listener.onHud(
            world.selectedBlock,
            world.blockCount,
            world.playerX.roundToInt(),
            world.playerZ.roundToInt()
        )

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                interact(e.x, e.y, place = false)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                interact(e.x, e.y, place = true)
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!twoFingerMode) {
                    yaw -= (-distanceX) * 0.008f
                    pitch = (pitch + (-distanceY) * 0.006f).coerceIn(0.20f, 1.25f)
                    requestFrame()
                }
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(
            context,
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    val factor = detector.scaleFactor
                    if (factor.isFinite() && factor > 0f) {
                        distance = (distance / factor).coerceIn(4.5f, 13f)
                        requestFrame()
                    }
                    return true
                }
            }
        )
    }

    fun onResumeGame() {
        renderThread?.setPaused(false)
        requestFrame()
    }

    fun onPauseGame() {
        renderThread?.setPaused(true)
    }

    fun select(type: BlockType) {
        synchronized(worldLock) { world.select(type) }
        emitHud()
        listener.onMessage("${type.label} block selected.")
        requestFrame()
    }

    fun moveKey(keyCode: Int) {
        synchronized(worldLock) {
            when (keyCode) {
                KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> world.move(1f, 0f, yaw)
                KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> world.move(-1f, 0f, yaw)
                KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> world.move(0f, -1f, yaw)
                KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> world.move(0f, 1f, yaw)
                else -> return
            }
        }
        emitHud()
        requestFrame()
    }

    fun resetWorld() {
        synchronized(worldLock) { world.generateWorld() }
        renderThread?.markAllDirty()
        emitHud()
        requestFrame()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_W,
            KeyEvent.KEYCODE_A,
            KeyEvent.KEYCODE_S,
            KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveKey(keyCode)
            KeyEvent.KEYCODE_1 -> select(BlockType.GRASS)
            KeyEvent.KEYCODE_2 -> select(BlockType.DIRT)
            KeyEvent.KEYCODE_3 -> select(BlockType.STONE)
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    twoFingerMode = true
                    centroid(event).also {
                        lastCentroidX = it.first
                        lastCentroidY = it.second
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount >= 2) {
                    val c = centroid(event)
                    val dx = c.first - lastCentroidX
                    val dy = c.second - lastCentroidY
                    lastCentroidX = c.first
                    lastCentroidY = c.second
                    if (!scaleDetector.isInProgress && (abs(dx) > 1f || abs(dy) > 1f)) {
                        synchronized(worldLock) {
                            world.move(
                                (-dy / 55f).coerceIn(-1f, 1f),
                                (dx / 55f).coerceIn(-1f, 1f),
                                yaw
                            )
                        }
                        emitHud()
                        requestFrame()
                    }
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_UP -> if (event.pointerCount <= 2) twoFingerMode = false
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> twoFingerMode = false
        }

        if (!twoFingerMode) gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        stopRenderer()
        renderThread = RenderThread(surface, width, height).also { it.start() }
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        renderThread?.resize(width, height)
        requestFrame()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        stopRenderer()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit

    override fun onDetachedFromWindow() {
        stopRenderer()
        super.onDetachedFromWindow()
    }

    private fun stopRenderer() {
        renderThread?.shutdown()
        renderThread = null
    }

    private fun requestFrame() {
        renderThread?.requestFrame()
    }

    private fun emitHud() {
        val snapshot = synchronized(worldLock) {
            HudSnapshot(
                world.selectedBlock,
                world.blockCount,
                world.playerX.roundToInt(),
                world.playerZ.roundToInt()
            )
        }
        listener.onHud(snapshot.selected, snapshot.count, snapshot.x, snapshot.z)
    }

    private fun interact(screenX: Float, screenY: Float, place: Boolean) {
        val camera = cameraSnapshot(width.coerceAtLeast(1), height.coerceAtLeast(1))
        val ray = camera.rayForScreen(screenX, screenY, width.coerceAtLeast(1), height.coerceAtLeast(1))

        val hit = synchronized(worldLock) { pickBlock(ray) }
        if (hit == null) {
            listener.onMessage("No block selected. Aim at the terrain.")
            return
        }

        var changed = false
        var changedX = hit.block.x
        var changedZ = hit.block.z

        synchronized(worldLock) {
            if (place) {
                val x = hit.block.x + hit.nx
                val y = hit.block.y + hit.ny
                val z = hit.block.z + hit.nz
                changed = world.place(x, y, z)
                changedX = x
                changedZ = z
                listener.onMessage(
                    if (changed) "${world.selectedBlock.label.lowercase()} block placed."
                    else "That space is occupied."
                )
            } else {
                if (hit.block.y == 0) {
                    listener.onMessage("The bottom foundation cannot be mined.")
                    return
                }
                changed = world.mine(hit.block)
                listener.onMessage(if (changed) "Block mined." else "Block could not be mined.")
            }
        }

        if (changed) {
            renderThread?.markChunksDirty(ChunkMesher.affectedChunks(changedX, changedZ))
            emitHud()
            requestFrame()
        }
    }

    private fun pickBlock(ray: Ray): RayHit? {
        var best: RayHit? = null
        for (block in world.allBlocks) {
            val intersection = rayBox(ray, block) ?: continue
            if (intersection.t > 22f) continue
            if (best == null || intersection.t < best!!.t) {
                best = RayHit(block, intersection.nx, intersection.ny, intersection.nz, intersection.t)
            }
        }
        return best
    }

    private fun rayBox(ray: Ray, block: Block): BoxHit? {
        val mins = floatArrayOf(block.x - 0.5f, block.y - 0.5f, block.z - 0.5f)
        val maxs = floatArrayOf(block.x + 0.5f, block.y + 0.5f, block.z + 0.5f)
        val origins = floatArrayOf(ray.ox, ray.oy, ray.oz)
        val dirs = floatArrayOf(ray.dx, ray.dy, ray.dz)

        var tMin = 0f
        var tMax = 1000f
        var nx = 0
        var ny = 0
        var nz = 0

        for (axis in 0..2) {
            val o = origins[axis]
            val d = dirs[axis]
            if (abs(d) < 0.00001f) {
                if (o < mins[axis] || o > maxs[axis]) return null
                continue
            }

            var t1 = (mins[axis] - o) / d
            var t2 = (maxs[axis] - o) / d
            var nearNormal = -1
            if (t1 > t2) {
                val tmp = t1; t1 = t2; t2 = tmp
                nearNormal = 1
            }

            if (t1 > tMin) {
                tMin = t1
                nx = 0; ny = 0; nz = 0
                when (axis) {
                    0 -> nx = nearNormal
                    1 -> ny = nearNormal
                    2 -> nz = nearNormal
                }
            }
            tMax = min(tMax, t2)
            if (tMin > tMax) return null
        }

        if (tMax < 0f) return null
        return BoxHit(if (tMin >= 0f) tMin else tMax, nx, ny, nz)
    }

    private fun cameraSnapshot(viewWidth: Int, viewHeight: Int): CameraInfo {
        val player = synchronized(worldLock) {
            floatArrayOf(world.playerX, world.playerY() + 0.5f, world.playerZ)
        }
        val horizontal = cos(pitch) * distance
        val ex = player[0] + sin(yaw) * horizontal
        val ey = player[1] + sin(pitch) * distance
        val ez = player[2] + cos(yaw) * horizontal

        var fx = player[0] - ex
        var fy = player[1] - ey
        var fz = player[2] - ez
        val fl = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(0.0001f)
        fx /= fl; fy /= fl; fz /= fl

        var rx = -fz
        var rz = fx
        val rl = sqrt(rx * rx + rz * rz).coerceAtLeast(0.0001f)
        rx /= rl; rz /= rl

        val ux = -rz * fy
        val uy = rz * fx - rx * fz
        val uz = rx * fy

        val projection = FloatArray(16)
        val view = FloatArray(16)
        val vp = FloatArray(16)
        Matrix.perspectiveM(
            projection,
            0,
            62f,
            viewWidth.toFloat() / viewHeight.coerceAtLeast(1).toFloat(),
            0.1f,
            60f
        )
        Matrix.setLookAtM(
            view, 0,
            ex, ey, ez,
            player[0], player[1], player[2],
            0f, 1f, 0f
        )
        Matrix.multiplyMM(vp, 0, projection, 0, view, 0)

        return CameraInfo(ex, ey, ez, fx, fy, fz, rx, 0f, rz, ux, uy, uz, vp)
    }

    private fun centroid(event: MotionEvent): Pair<Float, Float> {
        var x = 0f
        var y = 0f
        val count = event.pointerCount.coerceAtLeast(1)
        for (i in 0 until count) {
            x += event.getX(i)
            y += event.getY(i)
        }
        return x / count to y / count
    }

    private inner class RenderThread(
        private val texture: SurfaceTexture,
        initialWidth: Int,
        initialHeight: Int
    ) : Thread("TinyCraft-GPU") {
        private val running = AtomicBoolean(true)
        private val stateLock = Object()
        private val dirtyChunks = LinkedHashSet<ChunkCoord>()

        @Volatile private var paused = false
        @Volatile private var frameRequested = true
        @Volatile private var allDirty = true
        @Volatile private var viewportWidth = initialWidth.coerceAtLeast(1)
        @Volatile private var viewportHeight = initialHeight.coerceAtLeast(1)

        private var eglDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface = EGL14.EGL_NO_SURFACE
        private val gpuChunks = LinkedHashMap<ChunkCoord, GpuChunk>()

        private var program = 0
        private var aPosition = -1
        private var aColor = -1
        private var uVp = -1
        private var uModel = -1
        private var uUseModel = -1
        private var uEye = -1
        private var uFogColor = -1
        private var playerVbo = 0
        private var playerVertexCount = 0

        fun shutdown() {
            running.set(false)
            synchronized(stateLock) { stateLock.notifyAll() }
            if (Thread.currentThread() !== this) {
                try { join(700) } catch (_: InterruptedException) { interrupt() }
            }
        }

        fun setPaused(value: Boolean) {
            paused = value
            if (!value) requestFrame()
        }

        fun resize(width: Int, height: Int) {
            viewportWidth = width.coerceAtLeast(1)
            viewportHeight = height.coerceAtLeast(1)
            requestFrame()
        }

        fun requestFrame() {
            frameRequested = true
            synchronized(stateLock) { stateLock.notifyAll() }
        }

        fun markAllDirty() {
            allDirty = true
            requestFrame()
        }

        fun markChunksDirty(chunks: Set<ChunkCoord>) {
            synchronized(dirtyChunks) { dirtyChunks.addAll(chunks) }
            requestFrame()
        }

        override fun run() {
            try {
                setupEgl()
                setupGl()
                var lastFrameNs = 0L

                while (running.get()) {
                    if (paused) {
                        synchronized(stateLock) { stateLock.wait(80) }
                        continue
                    }

                    rebuildDirtyMeshes()

                    if (!frameRequested) {
                        synchronized(stateLock) { stateLock.wait(80) }
                        continue
                    }

                    val now = System.nanoTime()
                    val minFrameNs = 16_666_667L
                    val remaining = minFrameNs - (now - lastFrameNs)
                    if (remaining > 0 && lastFrameNs != 0L) {
                        val ms = remaining / 1_000_000L
                        val ns = (remaining % 1_000_000L).toInt()
                        try { sleep(ms, ns) } catch (_: InterruptedException) { interrupt() }
                    }

                    frameRequested = false
                    renderFrame()
                    if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                        throw IllegalStateException("eglSwapBuffers failed: 0x${Integer.toHexString(EGL14.eglGetError())}")
                    }
                    lastFrameNs = System.nanoTime()
                }
            } catch (error: Throwable) {
                post { onGpuFailure(error) }
            } finally {
                releaseGl()
            }
        }

        private fun setupEgl() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) error("No EGL display")
            val versions = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) error("EGL initialize failed")

            var config = chooseConfig(EGLExt.EGL_OPENGL_ES3_BIT_KHR)
            var version = 3
            if (config == null) {
                config = chooseConfig(EGL14.EGL_OPENGL_ES2_BIT)
                version = 2
            }
            if (config == null) error("No compatible EGL config")

            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                config,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, version, EGL14.EGL_NONE),
                0
            )
            if (eglContext == EGL14.EGL_NO_CONTEXT && version == 3) {
                config = chooseConfig(EGL14.EGL_OPENGL_ES2_BIT) ?: error("No ES2 EGL config")
                eglContext = EGL14.eglCreateContext(
                    eglDisplay,
                    config,
                    EGL14.EGL_NO_CONTEXT,
                    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                    0
                )
            }
            if (eglContext == EGL14.EGL_NO_CONTEXT) error("EGL context creation failed")

            val nativeSurface = Surface(texture)
            try {
                eglSurface = EGL14.eglCreateWindowSurface(
                    eglDisplay,
                    config,
                    nativeSurface,
                    intArrayOf(EGL14.EGL_NONE),
                    0
                )
            } finally {
                nativeSurface.release()
            }
            if (eglSurface == EGL14.EGL_NO_SURFACE) error("EGL window surface creation failed")
            if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                error("eglMakeCurrent failed")
            }
        }

        private fun chooseConfig(renderableType: Int): android.opengl.EGLConfig? {
            val attrs = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_DEPTH_SIZE, 16,
                EGL14.EGL_RENDERABLE_TYPE, renderableType,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
            val count = IntArray(1)
            val ok = EGL14.eglChooseConfig(eglDisplay, attrs, 0, configs, 0, 1, count, 0)
            return if (ok && count[0] > 0) configs[0] else null
        }

        private fun setupGl() {
            GLES20.glClearColor(0.561f, 0.780f, 0.910f, 1f)
            GLES20.glEnable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_CULL_FACE)

            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            aPosition = GLES20.glGetAttribLocation(program, "aPosition")
            aColor = GLES20.glGetAttribLocation(program, "aColor")
            uVp = GLES20.glGetUniformLocation(program, "uVp")
            uModel = GLES20.glGetUniformLocation(program, "uModel")
            uUseModel = GLES20.glGetUniformLocation(program, "uUseModel")
            uEye = GLES20.glGetUniformLocation(program, "uEye")
            uFogColor = GLES20.glGetUniformLocation(program, "uFogColor")

            val player = buildPlayerCube()
            playerVertexCount = player.size / CpuChunkMesh.FLOATS_PER_VERTEX
            val ids = IntArray(1)
            GLES20.glGenBuffers(1, ids, 0)
            playerVbo = ids[0]
            uploadBuffer(playerVbo, player)
        }

        private fun rebuildDirtyMeshes() {
            val coords: Set<ChunkCoord> = if (allDirty) {
                allDirty = false
                synchronized(worldLock) { ChunkMesher.chunksForWorld(world) }
            } else {
                synchronized(dirtyChunks) {
                    if (dirtyChunks.isEmpty()) return
                    dirtyChunks.toSet().also { dirtyChunks.clear() }
                }
            }

            if (coords.isEmpty() && gpuChunks.isNotEmpty() && allDirty) {
                gpuChunks.values.forEach { deleteVbo(it.vbo) }
                gpuChunks.clear()
                return
            }

            if (coords.isNotEmpty() && gpuChunks.isNotEmpty()) {
                val live = synchronized(worldLock) { ChunkMesher.chunksForWorld(world) }
                val stale = gpuChunks.keys.filter { it !in live }
                stale.forEach { key -> gpuChunks.remove(key)?.let { deleteVbo(it.vbo) } }
            }

            for (coord in coords) {
                val cpu = synchronized(worldLock) { ChunkMesher.build(world, coord) }
                gpuChunks.remove(coord)?.let { deleteVbo(it.vbo) }
                if (cpu.vertexCount == 0) continue

                val ids = IntArray(1)
                GLES20.glGenBuffers(1, ids, 0)
                uploadBuffer(ids[0], cpu.vertices)
                gpuChunks[coord] = GpuChunk(
                    ids[0],
                    cpu.vertexCount,
                    cpu.centerX,
                    cpu.centerY,
                    cpu.centerZ,
                    cpu.radius
                )
            }
        }

        private fun renderFrame() {
            val w = viewportWidth.coerceAtLeast(1)
            val h = viewportHeight.coerceAtLeast(1)
            GLES20.glViewport(0, 0, w, h)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
            GLES20.glUseProgram(program)

            val camera = cameraSnapshot(w, h)
            val frustum = Frustum(camera.vp)
            GLES20.glUniformMatrix4fv(uVp, 1, false, camera.vp, 0)
            GLES20.glUniform3f(uEye, camera.eyeX, camera.eyeY, camera.eyeZ)
            GLES20.glUniform3f(uFogColor, 0.561f, 0.780f, 0.910f)

            val identity = FloatArray(16).also { Matrix.setIdentityM(it, 0) }
            GLES20.glUniformMatrix4fv(uModel, 1, false, identity, 0)
            GLES20.glUniform1f(uUseModel, 0f)

            for (chunk in gpuChunks.values) {
                if (!frustum.containsSphere(chunk.cx, chunk.cy, chunk.cz, chunk.radius)) continue
                drawVbo(chunk.vbo, chunk.vertexCount)
            }

            val player = synchronized(worldLock) {
                floatArrayOf(world.playerX, world.playerY(), world.playerZ)
            }
            val model = FloatArray(16)
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, player[0], player[1], player[2])
            Matrix.scaleM(model, 0, 0.55f, 1.15f, 0.55f)
            GLES20.glUniformMatrix4fv(uModel, 1, false, model, 0)
            GLES20.glUniform1f(uUseModel, 1f)
            drawVbo(playerVbo, playerVertexCount)
        }

        private fun drawVbo(vbo: Int, vertexCount: Int) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            val stride = CpuChunkMesh.FLOATS_PER_VERTEX * 4
            GLES20.glEnableVertexAttribArray(aPosition)
            GLES20.glVertexAttribPointer(aPosition, 3, GLES20.GL_FLOAT, false, stride, 0)
            GLES20.glEnableVertexAttribArray(aColor)
            GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, 3 * 4)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        }

        private fun uploadBuffer(vbo: Int, vertices: FloatArray) {
            val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
            buffer.position(0)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
            GLES20.glBufferData(
                GLES20.GL_ARRAY_BUFFER,
                vertices.size * 4,
                buffer,
                GLES20.GL_STATIC_DRAW
            )
        }

        private fun deleteVbo(vbo: Int) {
            if (vbo != 0) GLES20.glDeleteBuffers(1, intArrayOf(vbo), 0)
        }

        private fun releaseGl() {
            try {
                if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglMakeCurrent(
                        eglDisplay,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_SURFACE,
                        EGL14.EGL_NO_CONTEXT
                    )
                }
                gpuChunks.values.forEach { deleteVbo(it.vbo) }
                gpuChunks.clear()
                deleteVbo(playerVbo)
                if (program != 0) GLES20.glDeleteProgram(program)
                if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglDisplay != EGL14.EGL_NO_DISPLAY && eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                if (eglDisplay != EGL14.EGL_NO_DISPLAY) EGL14.eglTerminate(eglDisplay)
            } catch (_: Throwable) {
                // Renderer shutdown must never crash the Activity.
            } finally {
                eglDisplay = EGL14.EGL_NO_DISPLAY
                eglContext = EGL14.EGL_NO_CONTEXT
                eglSurface = EGL14.EGL_NO_SURFACE
            }
        }

        private fun createProgram(vertexSource: String, fragmentSource: String): Int {
            val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
            val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
            val result = GLES20.glCreateProgram()
            GLES20.glAttachShader(result, vertex)
            GLES20.glAttachShader(result, fragment)
            GLES20.glLinkProgram(result)
            val linked = IntArray(1)
            GLES20.glGetProgramiv(result, GLES20.GL_LINK_STATUS, linked, 0)
            GLES20.glDeleteShader(vertex)
            GLES20.glDeleteShader(fragment)
            if (linked[0] == 0) {
                val log = GLES20.glGetProgramInfoLog(result)
                GLES20.glDeleteProgram(result)
                error("GPU program link failed: $log")
            }
            return result
        }

        private fun compileShader(type: Int, source: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == 0) {
                val log = GLES20.glGetShaderInfoLog(shader)
                GLES20.glDeleteShader(shader)
                error("GPU shader compile failed: $log")
            }
            return shader
        }
    }

    private fun buildPlayerCube(): FloatArray {
        val c = floatArrayOf(0.949f, 0.788f, 0.298f, 1f)
        val positions = arrayOf(
            floatArrayOf(-.5f,-.5f,-.5f), floatArrayOf(.5f,-.5f,-.5f), floatArrayOf(.5f,.5f,-.5f), floatArrayOf(-.5f,.5f,-.5f),
            floatArrayOf(-.5f,-.5f,.5f), floatArrayOf(.5f,-.5f,.5f), floatArrayOf(.5f,.5f,.5f), floatArrayOf(-.5f,.5f,.5f)
        )
        val faces = arrayOf(
            intArrayOf(0,1,2,0,2,3), intArrayOf(4,7,6,4,6,5),
            intArrayOf(0,3,7,0,7,4), intArrayOf(1,5,6,1,6,2),
            intArrayOf(3,2,6,3,6,7), intArrayOf(0,4,5,0,5,1)
        )
        val shades = floatArrayOf(.92f,.92f,.82f,.82f,1.08f,.70f)
        val out = FloatArray(36 * CpuChunkMesh.FLOATS_PER_VERTEX)
        var n = 0
        for (f in faces.indices) {
            val shade = shades[f]
            for (index in faces[f]) {
                val p = positions[index]
                out[n++] = p[0]; out[n++] = p[1]; out[n++] = p[2]
                out[n++] = min(1f, c[0] * shade)
                out[n++] = min(1f, c[1] * shade)
                out[n++] = min(1f, c[2] * shade)
                out[n++] = 1f
            }
        }
        return out
    }

    private data class HudSnapshot(val selected: BlockType, val count: Int, val x: Int, val z: Int)
    private data class Ray(val ox: Float, val oy: Float, val oz: Float, val dx: Float, val dy: Float, val dz: Float)
    private data class BoxHit(val t: Float, val nx: Int, val ny: Int, val nz: Int)
    private data class RayHit(val block: Block, val nx: Int, val ny: Int, val nz: Int, val t: Float)
    private data class GpuChunk(val vbo: Int, val vertexCount: Int, val cx: Float, val cy: Float, val cz: Float, val radius: Float)

    private data class CameraInfo(
        val eyeX: Float,
        val eyeY: Float,
        val eyeZ: Float,
        val forwardX: Float,
        val forwardY: Float,
        val forwardZ: Float,
        val rightX: Float,
        val rightY: Float,
        val rightZ: Float,
        val upX: Float,
        val upY: Float,
        val upZ: Float,
        val vp: FloatArray
    ) {
        fun rayForScreen(x: Float, y: Float, width: Int, height: Int): Ray {
            val nx = 2f * x / width.coerceAtLeast(1) - 1f
            val ny = 1f - 2f * y / height.coerceAtLeast(1)
            val tanHalf = tan(Math.toRadians(62.0).toFloat() * 0.5f)
            val aspect = width.toFloat() / height.coerceAtLeast(1).toFloat()
            var dx = forwardX + rightX * nx * tanHalf * aspect + upX * ny * tanHalf
            var dy = forwardY + rightY * nx * tanHalf * aspect + upY * ny * tanHalf
            var dz = forwardZ + rightZ * nx * tanHalf * aspect + upZ * ny * tanHalf
            val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(0.0001f)
            dx /= len; dy /= len; dz /= len
            return Ray(eyeX, eyeY, eyeZ, dx, dy, dz)
        }
    }

    private class Frustum(matrix: FloatArray) {
        private val planes = Array(6) { FloatArray(4) }

        init {
            setPlane(0, matrix[3] + matrix[0], matrix[7] + matrix[4], matrix[11] + matrix[8], matrix[15] + matrix[12])
            setPlane(1, matrix[3] - matrix[0], matrix[7] - matrix[4], matrix[11] - matrix[8], matrix[15] - matrix[12])
            setPlane(2, matrix[3] + matrix[1], matrix[7] + matrix[5], matrix[11] + matrix[9], matrix[15] + matrix[13])
            setPlane(3, matrix[3] - matrix[1], matrix[7] - matrix[5], matrix[11] - matrix[9], matrix[15] - matrix[13])
            setPlane(4, matrix[3] + matrix[2], matrix[7] + matrix[6], matrix[11] + matrix[10], matrix[15] + matrix[14])
            setPlane(5, matrix[3] - matrix[2], matrix[7] - matrix[6], matrix[11] - matrix[10], matrix[15] - matrix[14])
        }

        private fun setPlane(index: Int, a: Float, b: Float, c: Float, d: Float) {
            val length = sqrt(a * a + b * b + c * c).coerceAtLeast(0.0001f)
            planes[index][0] = a / length
            planes[index][1] = b / length
            planes[index][2] = c / length
            planes[index][3] = d / length
        }

        fun containsSphere(x: Float, y: Float, z: Float, radius: Float): Boolean {
            for (p in planes) {
                if (p[0] * x + p[1] * y + p[2] * z + p[3] < -radius) return false
            }
            return true
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec3 aPosition;
            attribute vec4 aColor;
            uniform mat4 uVp;
            uniform mat4 uModel;
            uniform float uUseModel;
            varying vec4 vColor;
            varying vec3 vWorld;
            void main() {
                vec4 raw = vec4(aPosition, 1.0);
                vec4 world = mix(raw, uModel * raw, uUseModel);
                vWorld = world.xyz;
                vColor = aColor;
                gl_Position = uVp * world;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 vColor;
            varying vec3 vWorld;
            uniform vec3 uEye;
            uniform vec3 uFogColor;
            void main() {
                float dist = distance(vWorld, uEye);
                float fog = clamp((dist - 14.0) / 20.0, 0.0, 1.0);
                gl_FragColor = vec4(mix(vColor.rgb, uFogColor, fog), vColor.a);
            }
        """
    }
}
