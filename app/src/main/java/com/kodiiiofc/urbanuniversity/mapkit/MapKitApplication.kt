package com.kodiiiofc.urbanuniversity.mapkit

import android.app.Application
import com.yandex.mapkit.MapKitFactory
import com.kodiiiofc.urbanuniversity.mapkit.BuildConfig


class MapKitApplication : Application() {

    private var haveApiKey = false

    override fun onCreate() {
        super.onCreate()
        setApiKey()
    }

    private fun setApiKey() {
        if (!haveApiKey)
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
    }
}