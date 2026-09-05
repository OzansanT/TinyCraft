package com.tinycraft.nativeandroid

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MainActivity : Activity(), GameUiListener {
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var gameSurface: GameSurfaceView
    private lateinit var selectedLabel: TextView
    private lateinit var blockCountLabel: TextView
    private lateinit var positionLabel: TextView
    private lateinit var messageLabel: TextView
    private lateinit var blockPicker: LinearLayout
    private lateinit var grassButton: Button
    private lateinit var dirtButton: Button
    private lateinit var stoneButton: Button

    private var messageReset: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = UiPalette.bg
        window.navigationBarColor = UiPalette.bg
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        gameSurface.onResume()
    }

    override fun onPause() {
        gameSurface.onPause()
        super.onPause()
    }

    private fun buildUi() {
        val narrow = resources.configuration.screenWidthDp <= 640
        val bodyPadding = dp(if (narrow) 10 else 18)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(UiPalette.bg)
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_NEVER
        }

        val app = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(bodyPadding, bodyPadding, bodyPadding, bodyPadding)
        }
        rootScroll.addView(app, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val toolbar = LinearLayout(this).apply {
            orientation = if (narrow) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val instructionFlow = FlowLayout(this).apply {
            horizontalGapPx = dp(8)
            verticalGapPx = dp(8)
        }
        listOf("WASD: Move", "Drag: Rotate", "Click: Mine", "Shift + Click: Place")
            .forEach { instructionFlow.addView(makeBadge(it)) }

        val resetButton = makeButton("Generate New World").apply {
            setOnClickListener { gameSurface.resetWorld() }
        }

        if (narrow) {
            toolbar.addView(instructionFlow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            toolbar.addView(resetButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(12)
            })
        } else {
            toolbar.addView(instructionFlow, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            toolbar.addView(resetButton, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(12)
            })
        }
        app.addView(toolbar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })

        blockPicker = LinearLayout(this).apply {
            orientation = if (narrow) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
        }
        grassButton = makeButton("1 · Grass")
        dirtButton = makeButton("2 · Dirt")
        stoneButton = makeButton("3 · Stone")
        grassButton.setOnClickListener { gameSurface.select(BlockType.GRASS) }
        dirtButton.setOnClickListener { gameSurface.select(BlockType.DIRT) }
        stoneButton.setOnClickListener { gameSurface.select(BlockType.STONE) }
        addPickerButton(grassButton, narrow, first = true)
        addPickerButton(dirtButton, narrow, first = false)
        addPickerButton(stoneButton, narrow, first = false)
        app.addView(blockPicker, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = dp(12)
        })

        val frame = FrameLayout(this).apply {
            background = rounded(UiPalette.border, 14f, UiPalette.border, 1)
            setPadding(dp(1), dp(1), dp(1), dp(1))
        }

        val screenHeightDp = resources.configuration.screenHeightDp
        val canvasHeightDp = max(420, min(620, (screenHeightDp * 0.70f).roundToInt()))
        gameSurface = GameSurfaceView(this, this).apply {
            setBackgroundColor(Color.BLACK)
        }
        frame.addView(gameSurface, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(canvasHeightDp)))

        val hud = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(11), dp(9), dp(11), dp(9))
            background = rounded(Color.argb(163, 0, 0, 0), 10f)
        }
        selectedLabel = makeHudText("Selected: Grass")
        blockCountLabel = makeHudText("Blocks: 0")
        positionLabel = makeHudText("Position: 0, 0")
        hud.addView(selectedLabel)
        hud.addView(blockCountLabel)
        hud.addView(positionLabel)
        frame.addView(hud, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START).apply {
            leftMargin = dp(12)
            topMargin = dp(12)
        })

        messageLabel = TextView(this).apply {
            text = "Click a block to start mining."
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.argb(163, 0, 0, 0), 10f)
        }
        frame.addView(messageLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = dp(12)
        })

        app.addView(frame, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(canvasHeightDp + 2)))

        val help = TextView(this).apply {
            text = "Keyboard shortcuts: 1, 2 and 3 change the selected block. The player automatically walks on top of the terrain."
            setTextColor(UiPalette.muted)
            textSize = 14f
            includeFontPadding = false
            setPadding(0, dp(10), 0, 0)
        }
        app.addView(help, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        setContentView(rootScroll)
        updatePicker(BlockType.GRASS)
        gameSurface.requestFocus()
    }

    private fun addPickerButton(button: Button, narrow: Boolean, first: Boolean) {
        val params = if (narrow) {
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        } else {
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        if (!first) {
            if (narrow) params.topMargin = dp(8) else params.leftMargin = dp(8)
        }
        blockPicker.addView(button, params)
    }

    private fun makeBadge(label: String) = TextView(this).apply {
        text = label
        setTextColor(UiPalette.text)
        textSize = 14f
        includeFontPadding = false
        setPadding(dp(12), dp(9), dp(12), dp(9))
        background = rounded(UiPalette.panel, 10f, UiPalette.border, 1)
    }

    private fun makeButton(label: String) = Button(this).apply {
        text = label
        setTextColor(UiPalette.text)
        textSize = 14f
        isAllCaps = false
        includeFontPadding = false
        minHeight = 0
        minWidth = 0
        setPadding(dp(12), dp(9), dp(12), dp(9))
        background = rounded(UiPalette.panel, 10f, UiPalette.border, 1)
        stateListAnimator = null
    }

    private fun makeHudText(label: String) = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 14f
        includeFontPadding = false
        typeface = Typeface.DEFAULT
    }

    private fun updatePicker(type: BlockType) {
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
            val reset = Runnable { messageLabel.text = "WASD to move · Drag to rotate" }
            messageReset = reset
            mainHandler.postDelayed(reset, 2400)
        }
    }

    private fun rounded(fill: Int, radiusDp: Float, stroke: Int? = null, strokeDp: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (stroke != null && strokeDp > 0) setStroke(dp(strokeDp), stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()
    private fun dp(value: Float): Int = (value * resources.displayMetrics.density).roundToInt()
}
