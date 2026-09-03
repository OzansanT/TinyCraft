package com.tinycraft.ui.hotbar

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.tinycraft.input.TouchLayout
import com.tinycraft.player.PlayerInventoryState
import com.tinycraft.rendering.BlockRenderPalette
import com.tinycraft.theme.GameColors
import com.tinycraft.theme.GameDimensions

/** Renders six player-owned hotbar slots. Input hit testing lives in HotbarController. */
class HotbarRenderer(private val inventory: PlayerInventoryState) : Disposable {
    private val shapes = ShapeRenderer()
    private val batch = SpriteBatch()
    private val font = BitmapFont()
    private val projection = Matrix4()
    private val center = Vector2()

    fun render() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        if (width <= 0f || height <= 0f) return

        projection.setToOrtho2D(0f, 0f, width, height)
        shapes.projectionMatrix = projection
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        val size = GameDimensions.HOTBAR_SLOT_SIZE
        val half = size * 0.5f
        val inset = GameDimensions.HOTBAR_BLOCK_INSET

        shapes.begin(ShapeRenderer.ShapeType.Filled)
        for (slot in 0 until PlayerInventoryState.HOTBAR_SIZE) {
            TouchLayout.hotbarSlotCenter(slot, width, height, center)
            val y = height - center.y
            shapes.color = GameColors.UI_SURFACE
            shapes.rect(center.x - half, y - half, size, size)

            inventory.slot(slot)?.let { stack ->
                shapes.color = BlockRenderPalette.color(stack.blockId)
                shapes.rect(
                    center.x - half + inset,
                    y - half + inset,
                    size - inset * 2f,
                    size - inset * 2f
                )
            }
        }
        shapes.end()

        Gdx.gl.glLineWidth(GameDimensions.HUD_LINE_WIDTH)
        shapes.begin(ShapeRenderer.ShapeType.Line)
        for (slot in 0 until PlayerInventoryState.HOTBAR_SIZE) {
            TouchLayout.hotbarSlotCenter(slot, width, height, center)
            val y = height - center.y
            shapes.color = if (slot == inventory.selectedSlotIndex) GameColors.UI_PRIMARY else GameColors.UI_TEXT_MUTED
            shapes.rect(center.x - half, y - half, size, size)
        }
        shapes.end()

        batch.projectionMatrix = projection
        batch.begin()
        font.color = GameColors.UI_TEXT
        for (slot in 0 until PlayerInventoryState.HOTBAR_SIZE) {
            val stack = inventory.slot(slot) ?: continue
            TouchLayout.hotbarSlotCenter(slot, width, height, center)
            val y = height - center.y
            font.draw(batch, stack.count.toString(), center.x + half - 20f, y - half + 18f)
        }
        batch.end()
    }

    override fun dispose() {
        font.dispose()
        batch.dispose()
        shapes.dispose()
    }
}
