package com.example.cityapp

import android.app.Application
import com.example.cityapp.di.ServiceLocator
import org.osmdroid.config.Configuration

class CityAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
        ServiceLocator.init(applicationContext)
    }
}
