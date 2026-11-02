package com.example.varsivo

import android.app.Application
import android.util.Log
import com.example.varsivo.api.RetrofitClient
import com.example.varsivo.data.ApplicationRepository
import com.example.varsivo.data.ApplicationsDb
import com.example.varsivo.data.AuthRepository
import com.example.varsivo.data.UserStore

/**
 * Application class: creates the shared data objects once so every Fragment uses the same
 * instances (simple manual dependency injection).
 */
class VarsivoApplication : Application() {

    val userStore: UserStore by lazy { UserStore(this) }

    val authRepository: AuthRepository by lazy {
        AuthRepository(userStore, RetrofitClient.userApi)
    }

    val applicationRepository: ApplicationRepository by lazy {
        ApplicationRepository(ApplicationsDb(this), RetrofitClient.applicationApi, userStore)
    }

    override fun onCreate() {
        super.onCreate()
        Log.i("VarsivoApplication", "Varsivo started")
    }
}
