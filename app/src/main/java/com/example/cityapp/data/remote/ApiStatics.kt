package com.example.cityapp.data.remote

/**
 * Публічний origin для відносних шляхів `/uploads/...` з API (узгоджено з Retrofit baseUrl у ServiceLocator).
 */
object ApiStatics {
    const val PUBLIC_ORIGIN: String = "http://10.0.2.2:3000"

    fun resolveMediaUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        val base = PUBLIC_ORIGIN.trimEnd('/')
        val p = if (path.startsWith("/")) path else "/$path"
        return base + p
    }
}
