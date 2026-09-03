package com.tinycraft.rendering

enum class FaceDirection(val dx: Int, val dy: Int, val dz: Int, val shade: Float) {
    UP(0, 1, 0, 1.00f),
    DOWN(0, -1, 0, 0.58f),
    NORTH(0, 0, -1, 0.78f),
    SOUTH(0, 0, 1, 0.86f),
    WEST(-1, 0, 0, 0.72f),
    EAST(1, 0, 0, 0.82f)
}
