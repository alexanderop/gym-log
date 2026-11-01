package dev.alexop.gymlog

import android.app.Application
import dev.alexop.gymlog.data.GymDatabase
import dev.alexop.gymlog.data.GymRepository
import kotlinx.coroutines.runBlocking

class GymLogApp : Application() {
    lateinit var repository: GymRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = GymDatabase.build(this)
        repository = GymRepository(database)
        runBlocking { repository.seedIfEmpty() }
    }
}
