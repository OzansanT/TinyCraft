package com.tinycraft.world.generation

import kotlin.math.floor

/** Small deterministic value-noise helper. Same seed + coordinates always return the same value. */
object TerrainNoise {
    fun sample(worldX: Int, worldZ: Int, seed: Long, scale: Int): Float {
        require(scale > 0) { "scale must be positive" }

        val x0 = Math.floorDiv(worldX, scale)
        val z0 = Math.floorDiv(worldZ, scale)
        val x1 = x0 + 1
        val z1 = z0 + 1

        val tx = Math.floorMod(worldX, scale).toFloat() / scale.toFloat()
        val tz = Math.floorMod(worldZ, scale).toFloat() / scale.toFloat()
        val sx = smooth(tx)
        val sz = smooth(tz)

        val a = lerp(hash01(x0, z0, seed), hash01(x1, z0, seed), sx)
        val b = lerp(hash01(x0, z1, seed), hash01(x1, z1, seed), sx)
        return lerp(a, b, sz)
    }

    private fun smooth(value: Float): Float = value * value * (3f - 2f * value)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun hash01(x: Int, z: Int, seed: Long): Float {
        var value = seed
        value = value xor (x.toLong() * 0x632BE59BD9B4E019L)
        value = value xor (z.toLong() * 0x9E3779B97F4A7C15UL.toLong())
        value = value xor (value ushr 30)
        value *= 0xBF58476D1CE4E5B9UL.toLong()
        value = value xor (value ushr 27)
        value *= 0x94D049BB133111EBUL.toLong()
        value = value xor (value ushr 31)

        val positive = value ushr 40
        return positive.toFloat() / 16_777_215f
    }
}
