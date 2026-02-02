package com.poc.search

import android.app.Application
import com.poc.search.data.db.AppDatabase

class App : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.build(this)
    }
}
