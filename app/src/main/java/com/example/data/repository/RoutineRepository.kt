package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Routine
import com.example.data.model.RoutineLog
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RoutineRepository(private val db: FirebaseFirestore) {

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

    fun observeRoutines(): Flow<List<Routine>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/routines"
        emitAll(
            db.collection("users").document(uid).collection("routines")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(Routine::class.java) }
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

    fun observeRoutineLogs(): Flow<List<RoutineLog>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/routine_logs"
        emitAll(
            db.collection("users").document(uid).collection("routine_logs")
                .snapshots()
                .map { snapshot -> snapshot.toObjects(RoutineLog::class.java) }
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

    suspend fun getRoutineById(routineId: String): Result<Routine> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
        return try {
            val snapshot = docRef.get().await()
            val routine = snapshot.toObject(Routine::class.java)
                ?: return Result.failure(NoSuchElementException("Routine not found"))
            Result.success(routine)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createRoutine(
        title: String,
        description: String? = null,
        timeOfDay: String = "morning",
        targetTime: String? = null,
        daysOfWeek: List<String> = emptyList(),
        steps: List<String> = emptyList()
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document()
        val payload = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to title,
            "timeOfDay" to timeOfDay,
            "daysOfWeek" to daysOfWeek,
            "steps" to steps,
            "currentStreak" to 0,
            "bestStreak" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!description.isNullOrBlank()) payload["description"] = description
        if (!targetTime.isNullOrBlank()) payload["targetTime"] = targetTime

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

    suspend fun updateRoutine(routine: Routine): Result<Unit> {
        val uid = requireUserId()
        require(routine.id.isNotBlank()) { "Routine ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("routines").document(routine.id)
        val payload = mutableMapOf<String, Any?>(
            "title" to routine.title,
            "timeOfDay" to routine.timeOfDay,
            "daysOfWeek" to routine.daysOfWeek,
            "steps" to routine.steps,
            "currentStreak" to routine.currentStreak,
            "bestStreak" to routine.bestStreak,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (routine.description != null) payload["description"] = routine.description
        if (routine.targetTime != null) payload["targetTime"] = routine.targetTime
        if (routine.lastCompletedDate != null) payload["lastCompletedDate"] = routine.lastCompletedDate

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

    suspend fun completeRoutine(
        routineId: String,
        currentRoutine: Routine,
        stepsCompleted: Int,
        totalSteps: Int,
        todayDateString: String // YYYY-MM-DD
    ): Result<Unit> {
        val uid = requireUserId()
        val routineDocRef = db.collection("users").document(uid).collection("routines").document(routineId)
        val logDocRef = db.collection("users").document(uid).collection("routine_logs").document()

        // Calculate streak
        val newStreak = if (currentRoutine.lastCompletedDate == todayDateString) {
            currentRoutine.currentStreak
        } else {
            currentRoutine.currentStreak + 1
        }
        val newBest = maxOf(currentRoutine.bestStreak, newStreak)

        val routineUpdates = mutableMapOf<String, Any?>(
            "currentStreak" to newStreak,
            "bestStreak" to newBest,
            "lastCompletedDate" to todayDateString,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        val logPayload = mapOf(
            "userId" to uid,
            "routineId" to routineId,
            "completedDate" to todayDateString,
            "stepsCompleted" to stepsCompleted,
            "totalSteps" to totalSteps,
            "completedAt" to FieldValue.serverTimestamp()
        )

        return try {
            db.runBatch { batch ->
                batch.update(routineDocRef, routineUpdates)
                batch.set(logDocRef, logPayload)
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, routineDocRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteRoutine(routineId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("routines").document(routineId)
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
