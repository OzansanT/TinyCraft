package com.tinycraft.blocks

/** Central block metadata registry. Systems query definitions instead of hardcoding block rules. */
object BlockRegistry {
    private val definitions = listOf(
        BlockDefinition(BlockId.AIR, "Air", solid = false, transparent = true, hardness = 0f),
        BlockDefinition(BlockId.GRASS, "Grass", solid = true, transparent = false, hardness = 0.6f),
        BlockDefinition(BlockId.DIRT, "Dirt", solid = true, transparent = false, hardness = 0.5f),
        BlockDefinition(BlockId.STONE, "Stone", solid = true, transparent = false, hardness = 1.5f),
        BlockDefinition(BlockId.SAND, "Sand", solid = true, transparent = false, hardness = 0.5f),
        BlockDefinition(BlockId.WOOD, "Wood", solid = true, transparent = false, hardness = 1.0f),
        BlockDefinition(BlockId.LEAVES, "Leaves", solid = true, transparent = true, hardness = 0.2f),
        BlockDefinition(BlockId.WATER, "Water", solid = false, transparent = true, hardness = 0f)
    ).associateBy(BlockDefinition::id)

    fun get(id: BlockId): BlockDefinition = requireNotNull(definitions[id]) {
        "Missing BlockDefinition for $id"
    }
}
