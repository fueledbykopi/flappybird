package com.mybird.game.data

import android.content.Context
import android.content.SharedPreferences
import com.mybird.game.engine.GameTheme

/**
 * Lightweight SharedPreferences store for high score, audio toggle, selected theme,
 * and persisted SAF/Photo Picker URIs for both custom bird & custom background.
 */
class GamePreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBestScore(): Int = prefs.getInt(KEY_BEST_SCORE, 0)

    fun saveBestScore(score: Int) {
        if (score > getBestScore()) {
            prefs.edit().putInt(KEY_BEST_SCORE, score).apply()
        }
    }

    fun isSoundEnabled(): Boolean = prefs.getBoolean(KEY_SOUND_ENABLED, true)

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun getSelectedTheme(): GameTheme =
        GameTheme.fromId(prefs.getString(KEY_SELECTED_THEME, GameTheme.DEFAULT.id))

    fun setSelectedTheme(theme: GameTheme) {
        prefs.edit().putString(KEY_SELECTED_THEME, theme.id).apply()
    }

    fun getCustomBirdPhotoUri(): String? = prefs.getString(KEY_CUSTOM_BIRD_URI, null)

    fun setCustomBirdPhotoUri(uriString: String?) {
        prefs.edit().apply {
            if (uriString == null) remove(KEY_CUSTOM_BIRD_URI)
            else putString(KEY_CUSTOM_BIRD_URI, uriString)
        }.apply()
    }

    fun getCustomBackgroundUri(): String? = prefs.getString(KEY_CUSTOM_BG_URI, null)

    fun setCustomBackgroundUri(uriString: String?) {
        prefs.edit().apply {
            if (uriString == null) remove(KEY_CUSTOM_BG_URI)
            else putString(KEY_CUSTOM_BG_URI, uriString)
        }.apply()
    }

    companion object {
        private const val PREFS_NAME = "mybird_prefs"
        private const val KEY_BEST_SCORE = "best_score"
        private const val KEY_SOUND_ENABLED = "sound_enabled"
        private const val KEY_SELECTED_THEME = "selected_theme"
        private const val KEY_CUSTOM_BIRD_URI = "custom_bird_uri"
        private const val KEY_CUSTOM_BG_URI = "custom_bg_uri"
    }
}
