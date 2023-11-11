package com.example.olx

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDialog

class SharedPreferencesManager(context: Context) {

    private val preference = context.getSharedPreferences(
        context.packageName,
        MODE_PRIVATE
    )

    private val editor = preference.edit()

    private val keyTheme = "theme"

    var theme
        get() = preference.getInt(keyTheme,2)
        set(value) {
            editor.putInt(keyTheme, value)
            editor.commit()
        }

    val themeFlag = arrayOf(
        AppCompatDelegate.MODE_NIGHT_NO,
        AppCompatDelegate.MODE_NIGHT_YES,
        AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    )
}