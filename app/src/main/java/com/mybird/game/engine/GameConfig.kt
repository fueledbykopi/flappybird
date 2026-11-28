package com.mybird.game.engine

/**
 * Resolution-independent physics and world geometry constants.
 * All vertical measurements are expressed in normalized world units where
 * total screen height equals [WORLD_HEIGHT].
 */
object GameConfig {
    const val WORLD_HEIGHT = 1000f
    const val GROUND_Y = 860f
    const val MIN_WORLD_WIDTH = 420f
    const val MAX_WORLD_WIDTH = 920f
    const val DEFAULT_WORLD_WIDTH = 562.5f // 9:16 portrait baseline

    // Bird geometry & physics
    const val BIRD_X_RATIO = 0.28f
    const val BIRD_READY_Y_RATIO = 0.47f
    const val BIRD_RADIUS = 28f
    const val BIRD_HITBOX_SCALE = 0.80f // Slightly forgiving circular hitbox

    const val GRAVITY = 2150f
    const val FLAP_VELOCITY = -690f
    const val MAX_FALL_VELOCITY = 980f
    const val MIN_ROTATION_DEG = -26f
    const val MAX_ROTATION_DEG = 84f

    // Pipe geometry & progression
    const val PIPE_WIDTH = 92f
    const val PIPE_LIP_HEIGHT = 30f
    const val PIPE_LIP_OVERHANG = 7f
    const val PIPE_BASE_GAP = 228f
    const val PIPE_MIN_GAP = 166f
    const val PIPE_BASE_SPEED = 240f
    const val PIPE_MAX_SPEED = 360f
    const val PIPE_SPAWN_SPACING = 330f
    const val PIPE_MIN_EDGE_CLEARANCE = 110f
    const val DIFFICULTY_MAX_SCORE = 45f

    // Simulation safety & pools
    const val MAX_DELTA_TIME = 0.05f // Prevent tunneling after background resume
    const val PIPE_POOL_SIZE = 6
    const val PARTICLE_POOL_SIZE = 36
    const val AMBIENT_PARTICLE_COUNT = 22
    const val POPUP_POOL_SIZE = 6
    const val CLOUD_COUNT = 5

    // Step event bitmask flags (zero-allocation frame notifications)
    const val EVENT_NONE = 0
    const val EVENT_SCORED = 1 shl 0
    const val EVENT_CRASHED = 1 shl 1
}
