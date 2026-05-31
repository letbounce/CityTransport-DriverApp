package com.example.cityapp.data.remote

import com.example.cityapp.BuildConfig

/**
 * Публічний origin для відносних шляхів `/uploads/...` з API (узгоджено з Retrofit baseUrl у ServiceLocator).
 * Значення береться з `api.origin` у `local.properties` (див. app/build.gradle.kts).
 */
object ApiStatics {
    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("data:")) return path
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = BuildConfig.API_ORIGIN.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }
}
