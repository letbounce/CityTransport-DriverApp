package com.example.cityapp.di

import android.content.Context
import com.example.cityapp.data.remote.api.ApiService
import com.example.cityapp.data.repository.AuthRepositoryImpl
import com.example.cityapp.data.repository.IncidentRepositoryImpl
import com.example.cityapp.data.repository.TripMapRepositoryImpl
import com.example.cityapp.data.repository.RouteRepositoryImpl
import com.example.cityapp.data.repository.VehicleRepositoryImpl
import com.example.cityapp.data.repository.SessionStore
import com.example.cityapp.data.repository.TokenProvider
import com.example.cityapp.data.repository.WaybillRepositoryImpl
import com.example.cityapp.domain.repository.AuthRepository
import com.example.cityapp.domain.repository.IncidentRepository
import com.example.cityapp.domain.repository.RouteRepository
import com.example.cityapp.domain.repository.TripMapRepository
import com.example.cityapp.domain.repository.VehicleRepository
import com.example.cityapp.domain.repository.WaybillRepository
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.runBlocking

object ServiceLocator {
    private lateinit var context: Context
    private val tokenProvider = TokenProvider()

    private val authInterceptor = Interceptor { chain ->
        val token = tokenProvider.getToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        }
        chain.proceed(request)
    }

    private val okHttp by lazy {
        val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        OkHttpClient.Builder().addInterceptor(authInterceptor).addInterceptor(logger).build()
    }

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/")
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val api by lazy { retrofit.create(ApiService::class.java) }
    private val sessionStore by lazy { SessionStore(context) }

    lateinit var authRepository: AuthRepository
        private set
    lateinit var routeRepository: RouteRepository
        private set
    lateinit var vehicleRepository: VehicleRepository
        private set
    lateinit var waybillRepository: WaybillRepository
        private set
    lateinit var incidentRepository: IncidentRepository
        private set
    lateinit var tripMapRepository: TripMapRepository
        private set

    fun init(appContext: Context) {
        if (this::context.isInitialized) return
        context = appContext
        authRepository = AuthRepositoryImpl(api, sessionStore, tokenProvider)
        routeRepository = RouteRepositoryImpl(api)
        vehicleRepository = VehicleRepositoryImpl(api)
        waybillRepository = WaybillRepositoryImpl(api)
        incidentRepository = IncidentRepositoryImpl(api)
        tripMapRepository = TripMapRepositoryImpl(api)
        runBlocking {
            tokenProvider.setToken(sessionStore.token())
        }
    }
}
