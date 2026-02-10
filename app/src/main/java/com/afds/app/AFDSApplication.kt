package com.afds.app

import android.app.Application
import com.afds.app.data.local.SessionManager
import com.afds.app.data.remote.ApiClient

class AFDSApplication : Application() {

    lateinit var apiClient: ApiClient
        private set

    lateinit var sessionManager: SessionManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        apiClient = ApiClient()
        sessionManager = SessionManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        apiClient.close()
    }

    companion object {
        lateinit var instance: AFDSApplication
            private set
    }
}