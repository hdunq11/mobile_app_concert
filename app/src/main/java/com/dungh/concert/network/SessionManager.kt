package com.dungh.concert.network

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.dungh.concert.model.User

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("concert_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_INFO = "user_info"
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
    }

    fun updateAccessToken(accessToken: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, accessToken).apply()
    }

    fun saveUser(user: User) {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(User::class.java)
        val json = adapter.toJson(user)
        prefs.edit().putString(KEY_USER_INFO, json).apply()
    }

    fun fetchAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun fetchRefreshToken(): String? {
        return prefs.getString(KEY_REFRESH_TOKEN, null)
    }

    fun fetchUser(): User? {
        val json = prefs.getString(KEY_USER_INFO, null) ?: return null
        return try {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(User::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return fetchAccessToken() != null
    }

    fun logout() {
        prefs.edit().apply {
            remove(KEY_ACCESS_TOKEN)
            remove(KEY_REFRESH_TOKEN)
            remove(KEY_USER_INFO)
            apply()
        }
    }
}
