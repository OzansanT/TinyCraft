package com.tinycraft.save

/** Storage boundary for encoded save content. */
interface SaveRepository {
    fun load(): String?
    fun save(content: String)
}
