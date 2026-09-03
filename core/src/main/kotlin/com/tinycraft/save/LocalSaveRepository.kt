package com.tinycraft.save

import com.badlogic.gdx.Gdx

/** Uses libGDX local storage while keeping file I/O outside gameplay and UI code. */
class LocalSaveRepository(
    private val relativePath: String = DEFAULT_SAVE_PATH
) : SaveRepository {
    override fun load(): String? {
        val file = Gdx.files.local(relativePath)
        return if (file.exists()) file.readString("UTF-8") else null
    }

    override fun save(content: String) {
        Gdx.files.local(relativePath).writeString(content, false, "UTF-8")
    }

    companion object {
        const val DEFAULT_SAVE_PATH = "saves/world.tc"
    }
}
