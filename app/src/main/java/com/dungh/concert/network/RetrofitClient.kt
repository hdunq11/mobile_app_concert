package com.dungh.concert.network

import android.content.Context
import android.content.Intent
import com.dungh.concert.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val BASE_URL: String = BuildConfig.API_BASE_URL
    private var sessionManager: SessionManager? = null
    private var appContext: Context? = null

    @Volatile
    private var isRefreshing = false

    fun init(context: Context) {
        appContext = context.applicationContext
        sessionManager = SessionManager(context.applicationContext)
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val reqBuilder = chain.request().newBuilder()
        sessionManager?.fetchAccessToken()?.let { token ->
            reqBuilder.addHeader("Authorization", "Bearer $token")
        }
        chain.proceed(reqBuilder.build())
    }

    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        if (response.code != 401 || response.request.header("Authorization") == null) {
            return@Authenticator null
        }

        val refreshToken = sessionManager?.fetchRefreshToken() ?: run {
            sessionManager?.logout()
            broadcastLogout()
            return@Authenticator null
        }

        synchronized(this) {
            if (isRefreshing) return@Authenticator null
            isRefreshing = true
            try {
                val refreshClient = OkHttpClient.Builder().build()
                val jsonBody = """{"refresh":"$refreshToken"}"""
                val refreshRequest = Request.Builder()
                    .url("${BASE_URL}api/token/refresh/")
                    .post(jsonBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val refreshResponse = refreshClient.newCall(refreshRequest).execute()
                if (!refreshResponse.isSuccessful) {
                    sessionManager?.logout()
                    broadcastLogout()
                    return@Authenticator null
                }

                val json = refreshResponse.body?.string() ?: return@Authenticator null
                val accessMatch = Regex(""""access"\s*:\s*"([^"]+)"""").find(json)
                val newAccess = accessMatch?.groupValues?.get(1) ?: return@Authenticator null
                sessionManager?.updateAccessToken(newAccess)

                response.request.newBuilder()
                    .header("Authorization", "Bearer $newAccess")
                    .build()
            } catch (_: Exception) {
                sessionManager?.logout()
                broadcastLogout()
                null
            } finally {
                isRefreshing = false
            }
        }
    }

    private fun broadcastLogout() {
        appContext?.let { context ->
            val intent = Intent("com.dungh.concert.ACTION_LOGOUT").apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.sendBroadcast(intent)
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val concertApi: ConcertApi = retrofit.create(ConcertApi::class.java)
}
