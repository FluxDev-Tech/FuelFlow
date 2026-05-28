package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.AppRepository

class FuelFlowApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.dao()) }

    override fun onCreate() {
        super.onCreate()
    }
}
