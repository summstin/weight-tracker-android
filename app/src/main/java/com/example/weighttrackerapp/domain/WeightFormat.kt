package com.example.weighttrackerapp.domain

import java.util.Locale

// Formats a weight with one decimal place
fun formatWeight(weight: Double): String = String.format(Locale.US, "%.1f", weight)