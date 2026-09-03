package com.tinycraft.player

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.math.MathUtils
import com.tinycraft.config.PlayerConfig
import com.tinycraft.config.RenderingConfig

/** Owns presentation camera state derived from PlayerState; it never owns gameplay pose. */
class PlayerCameraController(private val player: PlayerState) {
    val camera = PerspectiveCamera(
        RenderingConfig.FIELD_OF_VIEW_DEGREES,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    ).apply {
        near = RenderingConfig.NEAR_PLANE
        far = RenderingConfig.FAR_PLANE
    }

    fun update() {
        val yaw = player.yawDegrees * MathUtils.degreesToRadians
        val pitch = player.pitchDegrees * MathUtils.degreesToRadians
        val cosPitch = MathUtils.cos(pitch)

        camera.position.set(player.position.x, player.position.y + PlayerConfig.EYE_HEIGHT, player.position.z)
        camera.direction.set(
            -MathUtils.sin(yaw) * cosPitch,
            MathUtils.sin(pitch),
            -MathUtils.cos(yaw) * cosPitch
        ).nor()
        camera.up.set(0f, 1f, 0f)
        camera.update()
    }

    fun resize(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
    }
}
