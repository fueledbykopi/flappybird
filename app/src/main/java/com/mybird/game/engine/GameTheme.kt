package com.mybird.game.engine

/**
 * Built-in visual, character, and audio themes for MyBird.
 *
 * Each theme provides its own cohesive world scenery, obstacle style, signature character,
 * ambient effects, and procedural sound pack. [BLOK_M] is the default theme.
 */
enum class GameTheme(
    val id: String,
    val displayName: String,
    val shortName: String,
    val subtitle: String,
    val characterName: String,
    val flapParticleColors: LongArray,
    val scoreParticleColors: LongArray,
    val crashParticleColors: LongArray
) {
    BLOK_M(
        id = "blok_m",
        displayName = "Blok M",
        shortName = "Blok M",
        subtitle = "Jaksel Night & MRT",
        characterName = "Merpati Jaksel",
        flapParticleColors = longArrayOf(0xFFFFD180, 0xFF4DD0E1),
        scoreParticleColors = longArrayOf(0xFFFFCA28, 0xFFFF6EC7),
        crashParticleColors = longArrayOf(0xFFFF7043, 0xFFFFCA28, 0xFF4DD0E1)
    ),
    GTA6(
        id = "gta6",
        displayName = "GTA 6",
        shortName = "GTA 6",
        subtitle = "Vice Neon Sunset",
        characterName = "Flamingo Vice",
        flapParticleColors = longArrayOf(0xFFFF6EC7, 0xFFFFD166),
        scoreParticleColors = longArrayOf(0xFF00F5D4, 0xFFFF6EC7),
        crashParticleColors = longArrayOf(0xFFFF4D8D, 0xFFFFBE0B, 0xFF00F5D4)
    ),
    CYBERPUNK(
        id = "cyberpunk",
        displayName = "Cyberpunk",
        shortName = "Cyberpunk",
        subtitle = "Night City 2077",
        characterName = "Cyber Falcon",
        flapParticleColors = longArrayOf(0xFF05D9E8, 0xFFFCEE0A),
        scoreParticleColors = longArrayOf(0xFFFCEE0A, 0xFF05D9E8),
        crashParticleColors = longArrayOf(0xFFFF2A6D, 0xFF05D9E8, 0xFFFCEE0A)
    ),
    KARHUTLA_BORNEO(
        id = "karhutla_borneo",
        displayName = "Karhutla Borneo",
        shortName = "Borneo",
        subtitle = "Kabut & Bara",
        characterName = "Enggang Borneo",
        flapParticleColors = longArrayOf(0xFFFF8F00, 0xFFFFCC80),
        scoreParticleColors = longArrayOf(0xFFFFD54F, 0xFFFF6D00),
        crashParticleColors = longArrayOf(0xFFFF3D00, 0xFFFFAB00, 0xFFFFE0B2)
    ),
    ORIGINAL(
        id = "original",
        displayName = "Original",
        shortName = "Original",
        subtitle = "Classic Arcade",
        characterName = "Burung Kuning",
        flapParticleColors = longArrayOf(0xFFFFF8D6, 0xFFFFFFFF),
        scoreParticleColors = longArrayOf(0xFFFFD54F, 0xFFFFF176),
        crashParticleColors = longArrayOf(0xFFFF7043, 0xFFFFD54F, 0xFFFFFFFF)
    );

    companion object {
        val DEFAULT: GameTheme = BLOK_M

        fun fromId(id: String?): GameTheme {
            if (id.isNullOrBlank()) return DEFAULT
            return entries.firstOrNull { it.id == id } ?: DEFAULT
        }
    }
}
