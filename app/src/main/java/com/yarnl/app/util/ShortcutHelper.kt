package com.yarnl.app.util

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.yarnl.app.MainActivity
import com.yarnl.app.R

object ShortcutHelper {

    private const val SHORTCUT_RECENT_PATTERN = "recent_pattern"

    fun updateRecentPatternShortcut(context: Context, patternId: String, patternName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("shortcut_action", "pattern")
            putExtra("pattern_id", patternId)
        }

        val shortcut = ShortcutInfoCompat.Builder(context, SHORTCUT_RECENT_PATTERN)
            .setShortLabel(patternName)
            .setLongLabel(patternName)
            .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_edit))
            .setIntent(intent)
            .build()

        ShortcutManagerCompat.setDynamicShortcuts(context, listOf(shortcut))
    }

    fun removeRecentPatternShortcut(context: Context) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(SHORTCUT_RECENT_PATTERN))
    }
}
