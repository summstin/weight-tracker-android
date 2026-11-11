package com.example.weighttrackerapp

import android.content.Context
import com.example.weighttrackerapp.data.DatabaseHelper
import com.example.weighttrackerapp.data.SqliteWeightRepository
import com.example.weighttrackerapp.data.WeightRepository
import com.example.weighttrackerapp.domain.AuthService
import com.example.weighttrackerapp.domain.WeightService
import com.example.weighttrackerapp.security.PasswordHasher
import com.example.weighttrackerapp.ui.SmsGoalNotifier

// Builds the app's objects in one place so every screen shares a single database connection
object ServiceLocator {
    private val hasher = PasswordHasher()

    @Volatile
    private var repository: WeightRepository? = null

    private fun repository(context: Context): WeightRepository =
        repository ?: synchronized(this) {
            repository ?: SqliteWeightRepository(DatabaseHelper(context.applicationContext, hasher))
                .also { repository = it }
        }

    fun authService(context: Context) = AuthService(repository(context), hasher)

    fun weightService(context: Context) =
        WeightService(repository(context), SmsGoalNotifier(context.applicationContext))
}