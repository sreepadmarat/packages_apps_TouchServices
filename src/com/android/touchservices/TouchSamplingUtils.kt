/*
 * SPDX-FileCopyrightText: 2026 kenway214
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.touchservices

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.android.touchservices.utils.FileUtils
import org.json.JSONObject

object TouchSamplingUtils {
    const val TOUCH_SAMPLING_FILE = "/proc/touchpanel/game_switch_enable"

    const val PREF_ENABLED = "touch_sampling_enabled"
    const val PREF_AUTO_ENABLE = "touch_sampling_auto_enable"
    const val PREF_AUTO_APPS = "touch_sampling_auto_apps"

    @JvmStatic
    fun isSupported(): Boolean = FileUtils.fileExists(TOUCH_SAMPLING_FILE)

    @JvmStatic
    fun isEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_ENABLED, false)
    }

    @JvmStatic
    fun isAutoEnabled(context: Context): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getBoolean(PREF_AUTO_ENABLE, false)
    }

    @JvmStatic
    fun getAutoApps(context: Context): Set<String> {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getStringSet(PREF_AUTO_APPS, emptySet()) ?: emptySet()
    }

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_ENABLED, enabled)
            .apply()
    }

    @JvmStatic
    fun setAutoEnabled(context: Context, enabled: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putBoolean(PREF_AUTO_ENABLE, enabled)
            .apply()
    }

    @JvmStatic
    fun setAutoApps(context: Context, apps: Set<String>) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putStringSet(PREF_AUTO_APPS, apps)
            .apply()
    }

    @JvmStatic
    fun setAppEnabled(context: Context, packageName: String, enabled: Boolean) {
        val updated = getAutoApps(context).toMutableSet()
        if (enabled) {
            updated.add(packageName)
        } else {
            updated.remove(packageName)
        }
        setAutoApps(context, updated)
    }

    @JvmStatic
    fun writeState(enabled: Boolean): Boolean = FileUtils.writeLine(TOUCH_SAMPLING_FILE, if (enabled) "1" else "0")

    @JvmStatic
    fun applyNow(context: Context, foregroundPackage: String? = null) {
        if (!isSupported()) return
        val effective = isEnabled(context) || (isAutoEnabled(context) && foregroundPackage != null && getAutoApps(context).contains(foregroundPackage))
        writeState(effective)
    }

    @JvmStatic
    fun syncService(context: Context) {
        val intent = Intent(context, TouchSamplingService::class.java)
        val shouldRun = isSupported() && (isEnabled(context) || isAutoEnabled(context))
        if (shouldRun) {
            context.startService(intent)
        } else {
            context.stopService(intent)
            applyNow(context, null)
        }
    }

    @JvmStatic
    fun getSettings(context: Context): JSONObject {
        return JSONObject()
            .put(PREF_ENABLED, isEnabled(context))
            .put(PREF_AUTO_ENABLE, isAutoEnabled(context))
            .put(PREF_AUTO_APPS, getAutoApps(context).toList())
    }

    @JvmStatic
    fun setSettings(context: Context, settings: JSONObject) {
        val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
        if (settings.has(PREF_ENABLED)) {
            editor.putBoolean(PREF_ENABLED, settings.optBoolean(PREF_ENABLED, false))
        }
        if (settings.has(PREF_AUTO_ENABLE)) {
            editor.putBoolean(PREF_AUTO_ENABLE, settings.optBoolean(PREF_AUTO_ENABLE, false))
        }
        if (settings.has(PREF_AUTO_APPS)) {
            val array = settings.optJSONArray(PREF_AUTO_APPS)
            val apps = mutableSetOf<String>()
            if (array != null) {
                for (index in 0 until array.length()) {
                    array.optString(index)?.takeIf { it.isNotBlank() }?.let(apps::add)
                }
            }
            editor.putStringSet(PREF_AUTO_APPS, apps)
        }
        editor.apply()
        syncService(context)
    }
}
