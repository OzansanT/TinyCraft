package com.tinycraft.nativeandroid

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

enum class BlockType(val label: String, val color: FloatArray) {
    GRASS("Grass", floatArrayOf(0.404f, 0.659f, 0.267f, 1f)),
    DIRT("Dirt", floatArrayOf(0.502f, 0.325f, 0.200f, 1f)),
    STONE("Stone", floatArrayOf(0.467f, 0.482f, 0.502f, 1f)),
    WOOD("Wood", floatArrayOf(0.439f, 0.271f, 0.133f, 1f)),
    LEAVES("Leaves", floatArrayOf(0.224f, 0.471f, 0.259f, 1f))
}

data class Block(val x: Int, val y: Int, val z: Int, val type: BlockType)

data class PickResult(
    val hit: Block,
    val placeX: Int,
    val placeY: Int,
    val placeZ: Int
)

class VoxelWorld {
    private val blocks = LinkedHashMap<String, Block>()
    private var seed = Random.nextFloat() * 1000f

    var selectedBlock: BlockType = BlockType.GRASS
        private set

    var playerX = 0f
        private set
    var playerZ = 0f
        private set

    val blockCount: Int get() = blocks.size
    val allBlocks: Collection<Block> get() = blocks.values

    init {
        generateWorld()
    }

    fun select(type: BlockType) {
        selectedBlock = type
    }

    fun generateWorld() {
        blocks.clear()
        seed = Random.nextFloat() * 1000f
        val radius = 7
        val treeCandidates = mutableListOf<Triple<Int, Int, Int>>()

        for (x in -radius..radius) {
            for (z in -radius..radius) {
                val height = terrainHeight(x, z)
                for (y in 0..height) {
                    val type = when {
                        y == height -> BlockType.GRASS
                        y >= height - 2 -> BlockType.DIRT
                        else -> BlockType.STONE
                    }
                    addBlock(x, y, z, type)
                }

                if (abs(x) > 2 && abs(z) > 2 && Random.nextFloat() < 0.025f) {
                    treeCandidates += Triple(x, height, z)
                }
            }
        }

        treeCandidates.take(4).forEach { (x, y, z) -> generateTree(x, y, z) }
        playerX = 0f
        playerZ = 0f
    }

    fun topHeight(x: Int, z: Int): Int {
        var top = -1
        blocks.values.forEach { block ->
            if (block.x == x && block.z == z) top = max(top, block.y)
        }
        return top
    }

    fun playerY(): Float = topHeight(playerX.roundToInt(), playerZ.roundToInt()) + 1.08f

    fun move(forward: Float, strafe: Float, yaw: Float) {
        val length = kotlin.math.sqrt(forward * forward + strafe * strafe)
        if (length <= 0.0001f) return
        val f = forward / max(1f, length)
        val s = strafe / max(1f, length)
        val speed = 0.34f

        val forwardX = -sin(yaw)
        val forwardZ = -cos(yaw)
        val rightX = cos(yaw)
        val rightZ = -sin(yaw)

        val nextX = playerX + (forwardX * f + rightX * s) * speed
        val nextZ = playerZ + (forwardZ * f + rightZ * s) * speed

        if (abs(nextX) > 6.8f || abs(nextZ) > 6.8f) return

        val currentHeight = topHeight(playerX.roundToInt(), playerZ.roundToInt())
        val nextHeight = topHeight(nextX.roundToInt(), nextZ.roundToInt())
        if (nextHeight >= 0 && nextHeight - currentHeight <= 1) {
            playerX = nextX
            playerZ = nextZ
        }
    }

    fun mine(block: Block): Boolean {
        if (block.y == 0) return false
        blocks.remove(key(block.x, block.y, block.z))
        return true
    }

    fun place(x: Int, y: Int, z: Int): Boolean {
        if (blocks.size > 1500) return false
        if (blocks.containsKey(key(x, y, z))) return false

        val dx = x - playerX
        val dy = y - playerY()
        val dz = z - playerZ
        if (dx * dx + dy * dy + dz * dz < 1.21f) return false

        return addBlock(x, y, z, selectedBlock)
    }

    fun blockAt(x: Int, y: Int, z: Int): Block? = blocks[key(x, y, z)]

    private fun addBlock(x: Int, y: Int, z: Int, type: BlockType): Boolean {
        val k = key(x, y, z)
        if (blocks.containsKey(k)) return false
        blocks[k] = Block(x, y, z, type)
        return true
    }

    private fun terrainHeight(x: Int, z: Int): Int {
        val wave = sin((x + seed) * 0.55f) + cos((z - seed) * 0.48f)
        val detail = sin((x + z) * 0.9f) * 0.45f
        return min(4, max(1, (2.2f + wave * 0.55f + detail).roundToInt()))
    }

    private fun generateTree(x: Int, groundY: Int, z: Int) {
        val trunkHeight = 3
        for (y in 1..trunkHeight) addBlock(x, groundY + y, z, BlockType.WOOD)

        for (dx in -1..1) {
            for (dz in -1..1) {
                addBlock(x + dx, groundY + trunkHeight, z + dz, BlockType.LEAVES)
                if (abs(dx) + abs(dz) < 2) {
                    addBlock(x + dx, groundY + trunkHeight + 1, z + dz, BlockType.LEAVES)
                }
            }
        }
    }

    private fun key(x: Int, y: Int, z: Int) = "$x|$y|$z"
}
