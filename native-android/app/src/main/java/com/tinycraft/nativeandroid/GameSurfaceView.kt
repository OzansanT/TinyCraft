package com.tinycraft.nativeandroid

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure Android Canvas voxel renderer.
 *
 * This deliberately does not use GLSurfaceView, SurfaceView, WebView, Three.js,
 * libGDX, or a separate EGL surface. The original native build could launch
 * successfully while some phones still displayed a black SurfaceView. Drawing
 * into the normal Android View hierarchy removes that failure mode entirely.
 */
class GameSurfaceView(
    context: Context,
    private val listener: GameUiListener
) : View(context) {

    private val world = VoxelWorld()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = resources.displayMetrics.density.coerceAtLeast(1f)
        color = Color.argb(80, 0, 0, 0)
    }

    private val renderedFaces = ArrayList<RenderedFace>(2048)
    private var yaw = (Math.PI * 0.25).toFloat()
    private var pitch = 0.55f
    private var distance = 8f

    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector
    private var twoFingerMode = false
    private var lastCentroidX = 0f
    private var lastCentroidY = 0f

    init {
        setBackgroundColor(Color.rgb(143, 199, 232))
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
                interact(e.x, e.y, false)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                interact(e.x, e.y, true)
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
                    invalidate()
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
                        invalidate()
                    }
                    return true
                }
            }
        )
    }

    // Kept so MainActivity lifecycle code can remain unchanged.
    fun onResume() = invalidate()
    fun onPause() = Unit

    fun resetWorld() {
        world.generateWorld()
        emitHud()
        listener.onMessage("New world generated. Click to mine; Shift + click to place.")
        invalidate()
    }

    fun select(type: BlockType) {
        world.select(type)
        emitHud()
        listener.onMessage("${type.label} block selected.")
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 1 || height <= 1) return

        canvas.drawColor(Color.rgb(143, 199, 232))
        renderedFaces.clear()

        val camera = CameraState.from(world, yaw, pitch, distance, width, height)
        val drawItems = ArrayList<DrawCube>(world.blockCount + 1)

        world.allBlocks.forEach { block ->
            val dx = block.x - camera.eyeX
            val dy = block.y - camera.eyeY
            val dz = block.z - camera.eyeZ
            drawItems += DrawCube(
                block = block,
                cx = block.x.toFloat(),
                cy = block.y.toFloat(),
                cz = block.z.toFloat(),
                sx = 1f,
                sy = 1f,
                sz = 1f,
                baseColor = blockColor(block.type),
                distanceSq = dx * dx + dy * dy + dz * dz,
                isPlayer = false
            )
        }

        val pdx = world.playerX - camera.eyeX
        val pdy = world.playerY() - camera.eyeY
        val pdz = world.playerZ - camera.eyeZ
        drawItems += DrawCube(
            block = null,
            cx = world.playerX,
            cy = world.playerY(),
            cz = world.playerZ,
            sx = 0.55f,
            sy = 1.15f,
            sz = 0.55f,
            baseColor = Color.rgb(242, 201, 76),
            distanceSq = pdx * pdx + pdy * pdy + pdz * pdz,
            isPlayer = true
        )

        // Painter's algorithm: far objects first, near objects last.
        drawItems.sortByDescending { it.distanceSq }
        drawItems.forEach { drawCube(canvas, camera, it) }
    }

    private fun drawCube(canvas: Canvas, camera: CameraState, cube: DrawCube) {
        val hx = cube.sx * 0.5f
        val hy = cube.sy * 0.5f
        val hz = cube.sz * 0.5f
        val vertices3 = arrayOf(
            P3(cube.cx - hx, cube.cy - hy, cube.cz - hz),
            P3(cube.cx + hx, cube.cy - hy, cube.cz - hz),
            P3(cube.cx + hx, cube.cy + hy, cube.cz - hz),
            P3(cube.cx - hx, cube.cy + hy, cube.cz - hz),
            P3(cube.cx - hx, cube.cy - hy, cube.cz + hz),
            P3(cube.cx + hx, cube.cy - hy, cube.cz + hz),
            P3(cube.cx + hx, cube.cy + hy, cube.cz + hz),
            P3(cube.cx - hx, cube.cy + hy, cube.cz + hz)
        )
        val projected = Array<P2?>(8) { camera.project(vertices3[it]) }

        val faces = FACES
        for (face in faces) {
            if (!cube.isPlayer && cube.block != null) {
                val neighbor = world.blockAt(
                    cube.block.x + face.nx,
                    cube.block.y + face.ny,
                    cube.block.z + face.nz
                )
                if (neighbor != null) continue
            }

            val centerX = cube.cx + face.nx * hx
            val centerY = cube.cy + face.ny * hy
            val centerZ = cube.cz + face.nz * hz
            val toCameraX = camera.eyeX - centerX
            val toCameraY = camera.eyeY - centerY
            val toCameraZ = camera.eyeZ - centerZ
            val facing = face.nx * toCameraX + face.ny * toCameraY + face.nz * toCameraZ
            if (facing <= 0f) continue

            val pts = FloatArray(8)
            var valid = true
            for (i in 0..3) {
                val p = projected[face.indices[i]]
                if (p == null) {
                    valid = false
                    break
                }
                pts[i * 2] = p.x
                pts[i * 2 + 1] = p.y
            }
            if (!valid) continue

            val path = Path().apply {
                moveTo(pts[0], pts[1])
                lineTo(pts[2], pts[3])
                lineTo(pts[4], pts[5])
                lineTo(pts[6], pts[7])
                close()
            }

            val shade = when {
                face.ny > 0 -> 1.08f
                face.nx != 0 -> 0.82f
                face.nz != 0 -> 0.92f
                else -> 0.70f
            }
            paint.color = shadeColor(cube.baseColor, shade)
            canvas.drawPath(path, paint)
            canvas.drawPath(path, edgePaint)

            if (!cube.isPlayer && cube.block != null) {
                renderedFaces += RenderedFace(cube.block, face.nx, face.ny, face.nz, pts)
            }
        }
    }

    private fun interact(x: Float, y: Float, place: Boolean) {
        // Faces are appended in draw order, so search back-to-front for the
        // visually topmost polygon under the finger.
        val face = renderedFaces.asReversed().firstOrNull { pointInPolygon(x, y, it.points) }
        if (face == null) {
            listener.onMessage("No block selected. Aim at the terrain.")
            return
        }

        if (place) {
            val placed = world.place(
                face.block.x + face.nx,
                face.block.y + face.ny,
                face.block.z + face.nz
            )
            listener.onMessage(
                if (placed) "${world.selectedBlock.label.lowercase()} block placed."
                else "That space is occupied."
            )
        } else {
            if (face.block.y == 0) {
                listener.onMessage("The bottom foundation cannot be mined.")
                return
            }
            world.mine(face.block)
            listener.onMessage("Block mined.")
        }
        emitHud()
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount >= 2) {
                    twoFingerMode = true
                    val c = centroid(event)
                    lastCentroidX = c.first
                    lastCentroidY = c.second
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
                        val forward = (-dy / 55f).coerceIn(-1f, 1f)
                        val strafe = (dx / 55f).coerceIn(-1f, 1f)
                        world.move(forward, strafe, yaw)
                        emitHud()
                        invalidate()
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> world.move(1f, 0f, yaw)
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> world.move(-1f, 0f, yaw)
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> world.move(0f, -1f, yaw)
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> world.move(0f, 1f, yaw)
            KeyEvent.KEYCODE_1 -> world.select(BlockType.GRASS)
            KeyEvent.KEYCODE_2 -> world.select(BlockType.DIRT)
            KeyEvent.KEYCODE_3 -> world.select(BlockType.STONE)
            else -> return super.onKeyDown(keyCode, event)
        }
        emitHud()
        invalidate()
        return true
    }

    private fun emitHud() {
        listener.onHud(
            world.selectedBlock,
            world.blockCount,
            world.playerX.roundToInt(),
            world.playerZ.roundToInt()
        )
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

    private fun blockColor(type: BlockType): Int = when (type) {
        BlockType.GRASS -> Color.rgb(103, 168, 68)
        BlockType.DIRT -> Color.rgb(128, 83, 51)
        BlockType.STONE -> Color.rgb(119, 123, 128)
        BlockType.WOOD -> Color.rgb(112, 69, 34)
        BlockType.LEAVES -> Color.rgb(57, 120, 66)
    }

    private fun shadeColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).roundToInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).roundToInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).roundToInt().coerceIn(0, 255)
        return Color.rgb(r, g, b)
    }

    private fun pointInPolygon(x: Float, y: Float, pts: FloatArray): Boolean {
        var inside = false
        var j = 3
        for (i in 0..3) {
            val xi = pts[i * 2]
            val yi = pts[i * 2 + 1]
            val xj = pts[j * 2]
            val yj = pts[j * 2 + 1]
            val intersects = ((yi > y) != (yj > y)) &&
                (x < (xj - xi) * (y - yi) / ((yj - yi).takeUnless { abs(it) < 0.0001f } ?: 0.0001f) + xi)
            if (intersects) inside = !inside
            j = i
        }
        return inside
    }

    private data class P3(val x: Float, val y: Float, val z: Float)
    private data class P2(val x: Float, val y: Float)

    private data class CameraState(
        val eyeX: Float,
        val eyeY: Float,
        val eyeZ: Float,
        val rightX: Float,
        val rightY: Float,
        val rightZ: Float,
        val upX: Float,
        val upY: Float,
        val upZ: Float,
        val forwardX: Float,
        val forwardY: Float,
        val forwardZ: Float,
        val focal: Float,
        val centerX: Float,
        val centerY: Float
    ) {
        fun project(p: P3): P2? {
            val dx = p.x - eyeX
            val dy = p.y - eyeY
            val dz = p.z - eyeZ
            val cameraX = dx * rightX + dy * rightY + dz * rightZ
            val cameraY = dx * upX + dy * upY + dz * upZ
            val cameraZ = dx * forwardX + dy * forwardY + dz * forwardZ
            if (cameraZ <= 0.08f) return null
            val scale = focal / cameraZ
            return P2(centerX + cameraX * scale, centerY - cameraY * scale)
        }

        companion object {
            fun from(
                world: VoxelWorld,
                yaw: Float,
                pitch: Float,
                distance: Float,
                width: Int,
                height: Int
            ): CameraState {
                val tx = world.playerX
                val ty = world.playerY() + 0.5f
                val tz = world.playerZ
                val horizontal = cos(pitch) * distance
                val ex = tx + sin(yaw) * horizontal
                val ey = ty + sin(pitch) * distance
                val ez = tz + cos(yaw) * horizontal

                var fx = tx - ex
                var fy = ty - ey
                var fz = tz - ez
                val fl = sqrt(fx * fx + fy * fy + fz * fz).coerceAtLeast(0.0001f)
                fx /= fl; fy /= fl; fz /= fl

                // right = forward x worldUp
                var rx = -fz
                var ry = 0f
                var rz = fx
                val rl = sqrt(rx * rx + rz * rz).coerceAtLeast(0.0001f)
                rx /= rl; rz /= rl

                // camera up = right x forward
                val ux = ry * fz - rz * fy
                val uy = rz * fx - rx * fz
                val uz = rx * fy - ry * fx

                val fovRadians = Math.toRadians(62.0).toFloat()
                val focal = (height * 0.5f) / kotlin.math.tan(fovRadians * 0.5f)
                return CameraState(
                    ex, ey, ez,
                    rx, 0f, rz,
                    ux, uy, uz,
                    fx, fy, fz,
                    focal,
                    width * 0.5f,
                    height * 0.5f
                )
            }
        }
    }

    private data class DrawCube(
        val block: Block?,
        val cx: Float,
        val cy: Float,
        val cz: Float,
        val sx: Float,
        val sy: Float,
        val sz: Float,
        val baseColor: Int,
        val distanceSq: Float,
        val isPlayer: Boolean
    )

    private data class RenderedFace(
        val block: Block,
        val nx: Int,
        val ny: Int,
        val nz: Int,
        val points: FloatArray
    )

    private data class FaceDef(
        val nx: Int,
        val ny: Int,
        val nz: Int,
        val indices: IntArray
    )

    companion object {
        private val FACES = arrayOf(
            FaceDef(0, 0, -1, intArrayOf(0, 1, 2, 3)),
            FaceDef(0, 0, 1, intArrayOf(4, 7, 6, 5)),
            FaceDef(-1, 0, 0, intArrayOf(0, 3, 7, 4)),
            FaceDef(1, 0, 0, intArrayOf(1, 5, 6, 2)),
            FaceDef(0, 1, 0, intArrayOf(3, 2, 6, 7)),
            FaceDef(0, -1, 0, intArrayOf(0, 4, 5, 1))
        )
    }
}
