package com.example.cityapp

import android.app.Application
import com.example.cityapp.di.ServiceLocator

class CityAppApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(applicationContext)
    }
}
