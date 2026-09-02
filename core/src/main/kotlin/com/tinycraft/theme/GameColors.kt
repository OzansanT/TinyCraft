package com.tinycraft.theme

import com.badlogic.gdx.graphics.Color

/** Single source of truth for globally reused TinyCraft colors. */
object GameColors {
    val SKY = Color(0x8FC7E8FF.toInt())
    val GRASS = Color(0x67A844FF)
    val DIRT = Color(0x805333FF.toInt())
    val STONE = Color(0x777B80FF)
    val SAND = Color(0xD7C28AFF.toInt())
    val WOOD = Color(0x704522FF)
    val LEAVES = Color(0x397842FF)
    val WATER = Color(0x3E78B2CC)

    val UI_BACKGROUND = Color(0x15191ECC)
    val UI_SURFACE = Color(0x252B33FF)
    val UI_PRIMARY = Color(0x6DAE45FF)
    val UI_TEXT = Color.WHITE.cpy()
    val UI_TEXT_MUTED = Color(0xB7C0CAFF.toInt())
}
