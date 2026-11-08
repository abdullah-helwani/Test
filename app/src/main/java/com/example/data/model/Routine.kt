package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Routine(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val timeOfDay: String = "morning", // morning, afternoon, evening, anytime
    val targetTime: String? = null, // e.g. "07:30"
    val daysOfWeek: List<String> = emptyList(), // MON, TUE, WED, THU, FRI, SAT, SUN
    val steps: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val lastCompletedDate: String? = null, // YYYY-MM-DD
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
