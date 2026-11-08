package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class RoutineLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val routineId: String = "",
    val completedDate: String = "",
    val stepsCompleted: Int = 0,
    val totalSteps: Int = 0,
    @ServerTimestamp val completedAt: Timestamp? = null
)
