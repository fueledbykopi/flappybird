package com.mybird.game.engine

enum class GamePhase {
    MENU,
    READY,
    PLAYING,
    PAUSED,
    GAME_OVER
}

enum class SkyTheme {
    DAY,
    SUNSET,
    NIGHT
}

enum class Medal(val label: String) {
    NONE(""),
    BRONZE("Perunggu"),
    SILVER("Perak"),
    GOLD("Emas"),
    PLATINUM("Platinum")
}

class BirdState(
    var x: Float = GameConfig.DEFAULT_WORLD_WIDTH * GameConfig.BIRD_X_RATIO,
    var y: Float = GameConfig.GROUND_Y * GameConfig.BIRD_READY_Y_RATIO,
    var velocityY: Float = 0f,
    var rotationDeg: Float = 0f,
    var wingPhase: Float = 0f,
    var beakOpen: Float = 0f,
    val radius: Float = GameConfig.BIRD_RADIUS
) {
    val hitboxRadius: Float
        get() = radius * GameConfig.BIRD_HITBOX_SCALE
}

class PipePair(
    var x: Float = 0f,
    var gapCenterY: Float = GameConfig.GROUND_Y * 0.5f,
    var gapHeight: Float = GameConfig.PIPE_BASE_GAP,
    var scored: Boolean = false,
    var active: Boolean = false
) {
    val topPipeBottomY: Float
        get() = gapCenterY - gapHeight * 0.5f

    val bottomPipeTopY: Float
        get() = gapCenterY + gapHeight * 0.5f
}

class Particle(
    var x: Float = 0f,
    var y: Float = 0f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var size: Float = 6f,
    var life: Float = 0f,
    var maxLife: Float = 0.5f,
    var colorArgb: Long = 0xFFFFFFFF,
    var active: Boolean = false
) {
    val alpha: Float
        get() = if (maxLife > 0f) (life / maxLife).coerceIn(0f, 1f) else 0f
}

class FloatingPopup(
    var x: Float = 0f,
    var y: Float = 0f,
    var vy: Float = -90f,
    var life: Float = 0f,
    var maxLife: Float = 0.75f,
    var text: String = "+1",
    var active: Boolean = false
) {
    val alpha: Float
        get() = if (maxLife > 0f) (life / maxLife).coerceIn(0f, 1f) else 0f
}

class CloudItem(
    var x: Float = 0f,
    var y: Float = 120f,
    var width: Float = 130f,
    var speedFactor: Float = 0.14f
)
