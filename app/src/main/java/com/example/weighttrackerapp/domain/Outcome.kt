package com.example.weighttrackerapp.domain

// Result type with message
sealed class Outcome<out T> {
    data class Success<out T>(val value: T) : Outcome<T>()
    data class Failure(val message: String) : Outcome<Nothing>()
}