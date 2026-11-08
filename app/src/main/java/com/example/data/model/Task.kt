package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String = "medium", // low, medium, high, urgent
    val category: String = "General",
    val dueDate: String? = null, // YYYY-MM-DD
    val dueTime: String? = null, // HH:mm
    val isCompleted: Boolean = false,
    @ServerTimestamp val completedAt: Timestamp? = null,
    val subtasks: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
