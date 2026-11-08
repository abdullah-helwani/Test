package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Task
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaskRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeTasks(): Flow<List<Task>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/tasks"
        emitAll(
            db.collection("users").document(uid).collection("tasks")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Task::class.java) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun getTaskById(taskId: String): Result<Task> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        return try {
            val snapshot = docRef.get().await()
            val task = snapshot.toObject(Task::class.java)
                ?: return Result.failure(NoSuchElementException("Task not found"))
            Result.success(task)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createTask(
        title: String,
        description: String? = null,
        priority: String = "medium",
        category: String = "General",
        dueDate: String? = null,
        dueTime: String? = null,
        subtasks: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document()
        val payload = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to title,
            "priority" to priority,
            "category" to category,
            "isCompleted" to false,
            "subtasks" to subtasks,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!description.isNullOrBlank()) payload["description"] = description
        if (!dueDate.isNullOrBlank()) payload["dueDate"] = dueDate
        if (!dueTime.isNullOrBlank()) payload["dueTime"] = dueTime

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        val uid = requireUserId()
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)
        val payload = mutableMapOf<String, Any?>(
            "title" to task.title,
            "priority" to task.priority,
            "category" to task.category,
            "isCompleted" to task.isCompleted,
            "subtasks" to task.subtasks,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.description != null) payload["description"] = task.description
        if (task.dueDate != null) payload["dueDate"] = task.dueDate
        if (task.dueTime != null) payload["dueTime"] = task.dueTime
        if (task.isCompleted) {
            payload["completedAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(taskId: String, isCompleted: Boolean): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        val updates = mutableMapOf<String, Any?>(
            "isCompleted" to isCompleted,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isCompleted) {
            updates["completedAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(taskId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
