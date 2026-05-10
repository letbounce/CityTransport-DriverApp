package com.example.cityapp.data.repository

class TokenProvider {
    @Volatile
    private var token: String? = null

    fun setToken(value: String?) {
        token = value
    }

    fun getToken(): String? = token
}
