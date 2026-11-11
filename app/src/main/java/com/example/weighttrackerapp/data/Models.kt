package com.example.weighttrackerapp.data

data class UserRecord(val id: Long, val username: String, val passwordHash: String)

data class WeightEntry(val id: Long, val date: String, val weight: Double)

// A user's goal weight and the optional phone number to text when it is reached
data class Goal(val weight: Double, val phoneNumber: String?)