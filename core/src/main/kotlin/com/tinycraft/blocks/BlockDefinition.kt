package com.tinycraft.blocks

data class BlockDefinition(
    val id: BlockId,
    val displayName: String,
    val solid: Boolean,
    val transparent: Boolean,
    val hardness: Float
)
