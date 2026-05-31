package com.example.cityapp

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.example.cityapp.di.ServiceLocator
import okhttp3.OkHttpClient
import org.osmdroid.config.Configuration

class CityAppApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(applicationContext, getSharedPreferences("osmdroid", MODE_PRIVATE))
        }
        ServiceLocator.init(applicationContext)
    }

    override fun newImageLoader(): ImageLoader {
        val userAgent = "$packageName/1.0 (Android; RoutePulse)"
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val withAgent = if (request.header("User-Agent") == null) {
                    request.newBuilder()
                        .header("User-Agent", userAgent)
                        .build()
                } else {
                    request
                }
                chain.proceed(withAgent)
            }
            .build()
        return ImageLoader.Builder(this)
            .okHttpClient(client)
            .build()
    }
}
