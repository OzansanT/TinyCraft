package com.tinycraft.blocks

/** Stable numeric block identifiers. IDs become part of save compatibility once saves ship. */
enum class BlockId(val value: Int) {
    AIR(0),
    GRASS(1),
    DIRT(2),
    STONE(3),
    SAND(4),
    WOOD(5),
    LEAVES(6),
    WATER(7);

    companion object {
        private val byValue = entries.associateBy(BlockId::value)

        fun fromValue(value: Int): BlockId = byValue[value] ?: AIR
    }
}
