package com.example.cityapp.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "session_store")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("jwt_token")
    private val driverNameKey = stringPreferencesKey("driver_name")

    suspend fun save(token: String, driverName: String) {
        context.dataStore.edit { prefs ->
            prefs[tokenKey] = token
            prefs[driverNameKey] = driverName
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun token(): String? = context.dataStore.data.map { it[tokenKey] }.first()

    suspend fun driverName(): String? = context.dataStore.data.map { it[driverNameKey] }.first()
}
