package com.tinycraft.nativeandroid

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import kotlin.math.abs

class GameSurfaceView(
    context: Context,
    listener: GameUiListener
) : GLSurfaceView(context) {

    private val renderer = VoxelRenderer(listener)
    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector

    private var twoFingerMode = false
    private var lastCentroidX = 0f
    private var lastCentroidY = 0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        isFocusable = true
        isFocusableInTouchMode = true

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                queueEvent { renderer.interact(e.x, e.y, false) }
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                queueEvent { renderer.interact(e.x, e.y, true) }
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!twoFingerMode) {
                    queueEvent { renderer.rotate(-distanceX, -distanceY) }
                }
                return true
            }
        })

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val factor = detector.scaleFactor
                if (factor.isFinite() && factor > 0f) {
                    queueEvent { renderer.zoom(factor) }
                }
                return true
            }
        })
    }

    fun resetWorld() = queueEvent { renderer.resetWorld() }
    fun select(type: BlockType) = queueEvent { renderer.selectBlock(type) }

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
                        queueEvent { renderer.move(forward, strafe) }
                    }
                    return true
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) twoFingerMode = false
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> twoFingerMode = false
        }

        if (!twoFingerMode) gestureDetector.onTouchEvent(event)
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> queueEvent { renderer.move(1f, 0f) }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> queueEvent { renderer.move(-1f, 0f) }
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> queueEvent { renderer.move(0f, -1f) }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> queueEvent { renderer.move(0f, 1f) }
            KeyEvent.KEYCODE_1 -> select(BlockType.GRASS)
            KeyEvent.KEYCODE_2 -> select(BlockType.DIRT)
            KeyEvent.KEYCODE_3 -> select(BlockType.STONE)
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
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
}
