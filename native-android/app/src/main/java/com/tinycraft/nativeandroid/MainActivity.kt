package com.tinycraft.nativeandroid

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.roundToInt

class MainActivity : Activity(), GameUiListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var gameSurface: GameSurfaceView
    private lateinit var selectedLabel: TextView
    private lateinit var blockCountLabel: TextView
    private lateinit var positionLabel: TextView
    private lateinit var messageLabel: TextView
    private lateinit var controlsOverlay: LinearLayout
    private lateinit var grassButton: Button
    private lateinit var dirtButton: Button
    private lateinit var stoneButton: Button

    private lateinit var messageLayoutParams: FrameLayout.LayoutParams
    private var messageReset: Runnable? = null
    private var autoHideControls: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterImmersiveMode()
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        enterImmersiveMode()
        gameSurface.onResume()
    }

    override fun onPause() {
        gameSurface.onPause()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    override fun onDestroy() {
        messageReset?.let(mainHandler::removeCallbacks)
        autoHideControls?.let(mainHandler::removeCallbacks)
        super.onDestroy()
    }

    private fun buildUi() {
        val root = SwipeRevealLayout(this) { show ->
            if (show) showControls() else hideControls()
        }.apply {
            setBackgroundColor(Color.rgb(143, 199, 232))
        }

        gameSurface = GameSurfaceView(this, this).apply {
            setBackgroundColor(Color.rgb(143, 199, 232))
        }
        root.addView(
            gameSurface,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        val hud = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(9), dp(11), dp(9))
            background = rounded(Color.argb(158, 0, 0, 0), 10f)
        }
        selectedLabel = makeHudText("Selected: Grass")
        blockCountLabel = makeHudText("Blocks: 0")
        positionLabel = makeHudText("Position: 0, 0")
        hud.addView(selectedLabel)
        hud.addView(blockCountLabel)
        hud.addView(positionLabel)
        root.addView(
            hud,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START
            ).apply {
                leftMargin = dp(12)
                topMargin = dp(12)
            }
        )

        messageLabel = TextView(this).apply {
            text = "Swipe up from the lower screen for controls."
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.argb(158, 0, 0, 0), 10f)
        }
        messageLayoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        ).apply {
            leftMargin = dp(12)
            rightMargin = dp(12)
            bottomMargin = dp(22)
        }
        root.addView(messageLabel, messageLayoutParams)

        controlsOverlay = buildControlsOverlay()
        root.addView(
            controlsOverlay,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            ).apply {
                leftMargin = dp(12)
                rightMargin = dp(12)
                bottomMargin = dp(14)
            }
        )
        controlsOverlay.visibility = View.GONE
        controlsOverlay.alpha = 0f

        setContentView(root)
        updatePicker(BlockType.GRASS)
        gameSurface.requestFocus()
    }

    private fun buildControlsOverlay(): LinearLayout {
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(Color.argb(218, 23, 29, 34), 14f, UiPalette.border, 1)
        }

        val dpad = FrameLayout(this)
        val dpadSize = dp(162)
        panel.addView(
            dpad,
            LinearLayout.LayoutParams(dpadSize, dp(136))
        )

        val up = makeControlButton("▲")
        val left = makeControlButton("◀")
        val down = makeControlButton("▼")
        val right = makeControlButton("▶")

        dpad.addView(up, dpadButtonParams(Gravity.TOP or Gravity.CENTER_HORIZONTAL))
        dpad.addView(left, dpadButtonParams(Gravity.BOTTOM or Gravity.START))
        dpad.addView(down, dpadButtonParams(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL))
        dpad.addView(right, dpadButtonParams(Gravity.BOTTOM or Gravity.END))

        bindMovement(up, KeyEvent.KEYCODE_W)
        bindMovement(left, KeyEvent.KEYCODE_A)
        bindMovement(down, KeyEvent.KEYCODE_S)
        bindMovement(right, KeyEvent.KEYCODE_D)

        val divider = View(this).apply {
            setBackgroundColor(Color.argb(90, 255, 255, 255))
        }
        panel.addView(
            divider,
            LinearLayout.LayoutParams(dp(1), dp(118)).apply {
                leftMargin = dp(14)
                rightMargin = dp(14)
            }
        )

        val blocks = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        grassButton = makeControlButton("Grass", 96)
        dirtButton = makeControlButton("Dirt", 96)
        stoneButton = makeControlButton("Stone", 96)

        grassButton.setOnClickListener {
            gameSurface.select(BlockType.GRASS)
            scheduleControlsHide()
        }
        dirtButton.setOnClickListener {
            gameSurface.select(BlockType.DIRT)
            scheduleControlsHide()
        }
        stoneButton.setOnClickListener {
            gameSurface.select(BlockType.STONE)
            scheduleControlsHide()
        }

        blocks.addView(grassButton)
        blocks.addView(dirtButton, LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(7)
        })
        blocks.addView(stoneButton, LinearLayout.LayoutParams(dp(96), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(7)
        })
        panel.addView(blocks)

        return panel
    }

    private fun dpadButtonParams(gravity: Int) = FrameLayout.LayoutParams(
        dp(52),
        dp(52),
        gravity
    )

    private fun bindMovement(button: Button, keyCode: Int) {
        var repeat: Runnable? = null
        button.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    autoHideControls?.let(mainHandler::removeCallbacks)
                    val action = object : Runnable {
                        override fun run() {
                            val now = SystemClock.uptimeMillis()
                            gameSurface.onKeyDown(
                                keyCode,
                                KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0)
                            )
                            mainHandler.postDelayed(this, 85)
                        }
                    }
                    repeat = action
                    action.run()
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    repeat?.let(mainHandler::removeCallbacks)
                    repeat = null
                    scheduleControlsHide()
                    true
                }

                else -> true
            }
        }
    }

    private fun showControls() {
        if (!::controlsOverlay.isInitialized) return
        autoHideControls?.let(mainHandler::removeCallbacks)

        if (controlsOverlay.visibility != View.VISIBLE) {
            controlsOverlay.visibility = View.VISIBLE
            controlsOverlay.alpha = 0f
            controlsOverlay.translationY = dp(36).toFloat()
            controlsOverlay.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start()
        }

        messageLayoutParams.bottomMargin = dp(174)
        messageLabel.layoutParams = messageLayoutParams
        scheduleControlsHide()
    }

    private fun hideControls() {
        if (!::controlsOverlay.isInitialized || controlsOverlay.visibility != View.VISIBLE) return
        autoHideControls?.let(mainHandler::removeCallbacks)

        controlsOverlay.animate()
            .alpha(0f)
            .translationY(dp(36).toFloat())
            .setDuration(150)
            .withEndAction {
                controlsOverlay.visibility = View.GONE
                controlsOverlay.translationY = 0f
            }
            .start()

        messageLayoutParams.bottomMargin = dp(22)
        messageLabel.layoutParams = messageLayoutParams
    }

    private fun scheduleControlsHide() {
        autoHideControls?.let(mainHandler::removeCallbacks)
        val hide = Runnable { hideControls() }
        autoHideControls = hide
        mainHandler.postDelayed(hide, 4500)
    }

    private fun makeControlButton(label: String, fixedWidthDp: Int? = null) = Button(this).apply {
        text = label
        setTextColor(UiPalette.text)
        textSize = 14f
        isAllCaps = false
        includeFontPadding = false
        minHeight = 0
        minWidth = 0
        setPadding(dp(10), dp(8), dp(10), dp(8))
        background = rounded(UiPalette.panel, 10f, UiPalette.border, 1)
        stateListAnimator = null
        if (fixedWidthDp != null) {
            layoutParams = LinearLayout.LayoutParams(dp(fixedWidthDp), ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    private fun makeHudText(label: String) = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 14f
        includeFontPadding = false
        typeface = Typeface.DEFAULT
    }

    private fun updatePicker(type: BlockType) {
        if (!::grassButton.isInitialized) return
        val buttons = mapOf(
            BlockType.GRASS to grassButton,
            BlockType.DIRT to dirtButton,
            BlockType.STONE to stoneButton
        )
        buttons.forEach { (blockType, button) ->
            button.background = if (blockType == type) {
                rounded(UiPalette.panel, 10f, UiPalette.accent, 2)
            } else {
                rounded(UiPalette.panel, 10f, UiPalette.border, 1)
            }
        }
    }

    override fun onHud(selected: BlockType, blockCount: Int, playerX: Int, playerZ: Int) {
        mainHandler.post {
            selectedLabel.text = "Selected: ${selected.label}"
            blockCountLabel.text = "Blocks: $blockCount"
            positionLabel.text = "Position: $playerX, $playerZ"
            updatePicker(selected)
        }
    }

    override fun onMessage(text: String) {
        mainHandler.post {
            messageReset?.let(mainHandler::removeCallbacks)
            messageLabel.text = text
            val reset = Runnable {
                messageLabel.text = "Swipe up from the lower screen for controls."
            }
            messageReset = reset
            mainHandler.postDelayed(reset, 2400)
        }
    }

    private fun enterImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                )
        }
    }

    private fun rounded(
        fill: Int,
        radiusDp: Float,
        stroke: Int? = null,
        strokeDp: Int = 0
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != null && strokeDp > 0) setStroke(dp(strokeDp), stroke)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density).roundToInt()
}

private class SwipeRevealLayout(
    context: Context,
    private val onSwipe: (Boolean) -> Unit
) : FrameLayout(context) {
    private var startX = 0f
    private var startY = 0f
    private var eligible = false
    private var triggered = false
    private val threshold = 84f * resources.displayMetrics.density

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                eligible = event.y >= height * 0.55f
                triggered = false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!triggered) {
                    val dx = event.x - startX
                    val dy = event.y - startY
                    if (eligible && dy < -threshold && abs(dy) > abs(dx) * 1.15f) {
                        triggered = true
                        onSwipe(true)
                    } else if (dy > threshold && abs(dy) > abs(dx) * 1.15f) {
                        triggered = true
                        onSwipe(false)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                eligible = false
                triggered = false
            }
        }
        return super.dispatchTouchEvent(event)
    }
}
