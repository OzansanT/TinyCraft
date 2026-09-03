package com.tinycraft.rendering

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import kotlin.math.min

/** Converts already-culled visible faces into one non-indexed GPU mesh per chunk. */
class ChunkMeshBuilder {
    fun build(faces: List<VisibleFace>): Mesh? {
        if (faces.isEmpty()) return null

        val floatsPerVertex = 4
        val verticesPerFace = 6
        val vertexCount = faces.size * verticesPerFace
        val vertices = FloatArray(vertexCount * floatsPerVertex)
        var cursor = 0

        for (face in faces) {
            val offsets = verticesFor(face.direction)
            val baseColor = BlockRenderPalette.color(face.blockId)
            val shade = face.direction.shade
            val packedColor = Color.toFloatBits(
                min(1f, baseColor.r * shade),
                min(1f, baseColor.g * shade),
                min(1f, baseColor.b * shade),
                baseColor.a
            )

            var index = 0
            while (index < offsets.size) {
                vertices[cursor++] = face.worldX + offsets[index]
                vertices[cursor++] = face.worldY + offsets[index + 1]
                vertices[cursor++] = face.worldZ + offsets[index + 2]
                vertices[cursor++] = packedColor
                index += 3
            }
        }

        return Mesh(
            true,
            vertexCount,
            0,
            VertexAttribute.Position(),
            VertexAttribute.ColorPacked()
        ).also { mesh ->
            mesh.setVertices(vertices)
        }
    }

    private fun verticesFor(direction: FaceDirection): FloatArray = when (direction) {
        FaceDirection.UP -> UP_VERTICES
        FaceDirection.DOWN -> DOWN_VERTICES
        FaceDirection.NORTH -> NORTH_VERTICES
        FaceDirection.SOUTH -> SOUTH_VERTICES
        FaceDirection.WEST -> WEST_VERTICES
        FaceDirection.EAST -> EAST_VERTICES
    }

    companion object {
        private val UP_VERTICES = floatArrayOf(
            0f, 1f, 0f, 1f, 1f, 0f, 1f, 1f, 1f,
            0f, 1f, 0f, 1f, 1f, 1f, 0f, 1f, 1f
        )
        private val DOWN_VERTICES = floatArrayOf(
            0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 0f,
            0f, 0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f
        )
        private val NORTH_VERTICES = floatArrayOf(
            0f, 0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f,
            0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f, 0f
        )
        private val SOUTH_VERTICES = floatArrayOf(
            0f, 0f, 1f, 1f, 0f, 1f, 1f, 1f, 1f,
            0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f, 1f
        )
        private val WEST_VERTICES = floatArrayOf(
            0f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 1f,
            0f, 0f, 0f, 0f, 1f, 1f, 0f, 1f, 0f
        )
        private val EAST_VERTICES = floatArrayOf(
            1f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 1f,
            1f, 0f, 0f, 1f, 1f, 0f, 1f, 1f, 1f
        )
    }
}
