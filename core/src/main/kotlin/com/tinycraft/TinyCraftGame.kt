package com.tinycraft

import com.badlogic.gdx.Game
import com.tinycraft.screens.GameScreen

/** Core application entry point. Platform launchers should only construct this class. */
class TinyCraftGame : Game() {
    override fun create() {
        setScreen(GameScreen())
    }
}
