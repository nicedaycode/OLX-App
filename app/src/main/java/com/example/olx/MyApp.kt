package com.example.olx

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApp: Application() {

    override fun onCreate() {
        super.onCreate()

        val sharedPreferencesManager = SharedPreferencesManager(this)
        AppCompatDelegate.setDefaultNightMode(sharedPreferencesManager.themeFlag[sharedPreferencesManager.theme])
    }
}