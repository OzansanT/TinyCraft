package com.tinycraft.save

/** Deterministic line-oriented codec. Parsing failures return null instead of partially restoring state. */
class SaveCodec {
    fun encode(data: SaveData): String = buildString {
        appendLine(HEADER)
        appendLine("version=${data.version}")
        appendLine("generation=${data.generationVersion}")
        appendLine("seed=${data.worldSeed}")
        appendLine(
            "player=${data.player.x},${data.player.y},${data.player.z}," +
                "${data.player.yawDegrees},${data.player.pitchDegrees}"
        )
        appendLine("selected=${data.selectedHotbarSlot}")
        data.hotbarSlots.sortedBy { it.slot }.forEach { slot ->
            appendLine("slot=${slot.slot},${slot.blockIdValue},${slot.count}")
        }
        data.modifications
            .sortedWith(compareBy<WorldModificationSaveData> { it.x }.thenBy { it.y }.thenBy { it.z })
            .forEach { edit ->
                appendLine("edit=${edit.x},${edit.y},${edit.z},${edit.blockIdValue}")
            }
    }

    fun decode(text: String): SaveData? = runCatching {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        require(lines.firstOrNull() == HEADER) { "Missing TinyCraft save header" }

        var version: Int? = null
        var generationVersion: Int? = null
        var worldSeed: Long? = null
        var player: PlayerSaveData? = null
        var selectedHotbarSlot: Int? = null
        val slots = mutableListOf<HotbarSlotSaveData>()
        val edits = mutableListOf<WorldModificationSaveData>()

        for (line in lines.drop(1)) {
            val key = line.substringBefore('=', missingDelimiterValue = "")
            val value = line.substringAfter('=', missingDelimiterValue = "")
            when (key) {
                "version" -> version = value.toInt()
                "generation" -> generationVersion = value.toInt()
                "seed" -> worldSeed = value.toLong()
                "player" -> {
                    val parts = value.split(',')
                    require(parts.size == 5)
                    player = PlayerSaveData(
                        parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat(),
                        parts[3].toFloat(), parts[4].toFloat()
                    )
                }
                "selected" -> selectedHotbarSlot = value.toInt()
                "slot" -> {
                    val parts = value.split(',')
                    require(parts.size == 3)
                    slots += HotbarSlotSaveData(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
                "edit" -> {
                    val parts = value.split(',')
                    require(parts.size == 4)
                    edits += WorldModificationSaveData(
                        parts[0].toInt(), parts[1].toInt(), parts[2].toInt(), parts[3].toInt()
                    )
                }
            }
        }

        SaveData(
            version = requireNotNull(version),
            generationVersion = requireNotNull(generationVersion),
            worldSeed = requireNotNull(worldSeed),
            player = requireNotNull(player),
            selectedHotbarSlot = requireNotNull(selectedHotbarSlot),
            hotbarSlots = slots,
            modifications = edits
        )
    }.getOrNull()

    companion object {
        private const val HEADER = "TINYCRAFT_SAVE"
    }
}
