package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRoutineRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Test Task",
                priority = "high",
                category = "Work"
            )
        }
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue(taskId.isNotEmpty())
    }

    @Test
    fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createTask(
                title = "Observable Task",
                priority = "medium",
                category = "General"
            ).getOrThrow()
        }

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks().first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emittedTasks.any { it.id == taskId })
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TaskRepository(firestore)
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createTask(
                title = "Alice Secret",
                priority = "urgent",
                category = "Personal"
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        var thrown: Exception? = null
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                com.google.android.gms.tasks.Tasks.await(
                    firestore.collection("users").document(aliceUid).collection("tasks").document(taskId).get()
                )
            }
        } catch (e: Exception) {
            thrown = e
        }
        val firestoreException = thrown?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException", firestoreException)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = TaskRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeTasks().first()
            }
            fail("Expected exception for unauthenticated user")
        } catch (e: Exception) {
            // Unauthenticated check before entering or permission denied
            assertTrue(e is IllegalStateException || e.unwrapFirestoreException()?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    @Test
    fun createRoutine_validPayload_createsDocumentAndReturnsId() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val routineRepo = RoutineRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            routineRepo.createRoutine(
                title = "Morning Stretch",
                timeOfDay = "morning",
                targetTime = "07:00",
                daysOfWeek = listOf("MON", "WED", "FRI"),
                steps = listOf("Hydrate", "5-min stretch", "Deep breath")
            )
        }
        assertTrue(createResult.isSuccess)
        val routineId = createResult.getOrThrow()
        assertTrue(routineId.isNotEmpty())
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
