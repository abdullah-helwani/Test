package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class TaskFilterStatus { ALL, ACTIVE, COMPLETED }
enum class TaskSortOption { DUE_DATE, PRIORITY, TITLE }

data class TaskFilterParams(
    val selectedCategory: String = "All",
    val filterStatus: TaskFilterStatus = TaskFilterStatus.ALL,
    val sortOption: TaskSortOption = TaskSortOption.PRIORITY,
    val searchQuery: String = ""
)

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val categories: List<String> = listOf("All", "Work", "Personal", "Health", "Shopping", "General"),
    val selectedCategory: String = "All",
    val filterStatus: TaskFilterStatus = TaskFilterStatus.ALL,
    val sortOption: TaskSortOption = TaskSortOption.PRIORITY,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val userMessage: String? = null
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val userId: String
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("All")
    private val _filterStatus = MutableStateFlow(TaskFilterStatus.ALL)
    private val _sortOption = MutableStateFlow(TaskSortOption.PRIORITY)
    private val _searchQuery = MutableStateFlow("")
    private val _userMessage = MutableStateFlow<String?>(null)

    private val _filterParams = combine(
        _selectedCategory,
        _filterStatus,
        _sortOption,
        _searchQuery
    ) { category, status, sort, query ->
        TaskFilterParams(category, status, sort, query)
    }

    val uiState: StateFlow<TaskUiState> = combine(
        repository.observeTasks().catch { emit(emptyList()) },
        _filterParams,
        _userMessage
    ) { tasks: List<Task>, params: TaskFilterParams, message: String? ->
        val categories = listOf("All") + tasks.map { it.category }.filter { it.isNotBlank() }.distinct()

        val filtered = tasks
            .filter { task ->
                if (params.selectedCategory == "All") true
                else task.category.equals(params.selectedCategory, ignoreCase = true)
            }
            .filter { task ->
                when (params.filterStatus) {
                    TaskFilterStatus.ALL -> true
                    TaskFilterStatus.ACTIVE -> !task.isCompleted
                    TaskFilterStatus.COMPLETED -> task.isCompleted
                }
            }
            .filter { task ->
                if (params.searchQuery.isBlank()) true
                else task.title.contains(params.searchQuery, ignoreCase = true) ||
                        (task.description?.contains(params.searchQuery, ignoreCase = true) == true)
            }
            .sortedWith { a, b ->
                when (params.sortOption) {
                    TaskSortOption.PRIORITY -> {
                        val pA = priorityWeight(a.priority)
                        val pB = priorityWeight(b.priority)
                        if (pA != pB) pB.compareTo(pA)
                        else a.title.compareTo(b.title, ignoreCase = true)
                    }
                    TaskSortOption.DUE_DATE -> {
                        val dateA = a.dueDate ?: "9999-99-99"
                        val dateB = b.dueDate ?: "9999-99-99"
                        dateA.compareTo(dateB)
                    }
                    TaskSortOption.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                }
            }

        TaskUiState(
            tasks = tasks,
            filteredTasks = filtered,
            categories = if (categories.size <= 1) listOf("All", "Work", "Personal", "Health", "General") else categories,
            selectedCategory = params.selectedCategory,
            filterStatus = params.filterStatus,
            sortOption = params.sortOption,
            searchQuery = params.searchQuery,
            isLoading = false,
            userMessage = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState(isLoading = true)
    )

    private fun priorityWeight(priority: String): Int = when (priority.lowercase(Locale.ROOT)) {
        "urgent" -> 4
        "high" -> 3
        "medium" -> 2
        else -> 1
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setFilterStatus(status: TaskFilterStatus) {
        _filterStatus.value = status
    }

    fun setSortOption(option: TaskSortOption) {
        _sortOption.value = option
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun createTask(
        title: String,
        description: String?,
        priority: String,
        category: String,
        dueDate: String?,
        dueTime: String?,
        subtasks: List<String>
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val result = repository.createTask(
                title = title.trim(),
                description = description?.takeIf { it.isNotBlank() },
                priority = priority,
                category = category.trim().ifBlank { "General" },
                dueDate = dueDate?.takeIf { it.isNotBlank() },
                dueTime = dueTime?.takeIf { it.isNotBlank() },
                subtasks = subtasks.filter { it.isNotBlank() }
            )
            if (result.isSuccess) {
                _userMessage.value = "Task created!"
            } else {
                _userMessage.value = "Failed to create task"
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val result = repository.updateTask(task)
            if (result.isFailure) {
                _userMessage.value = "Failed to update task"
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val newStatus = !task.isCompleted
            val result = repository.toggleTaskCompletion(task.id, newStatus)
            if (result.isSuccess) {
                _userMessage.value = if (newStatus) "Task completed! 🎉" else "Task reopened"
            } else {
                _userMessage.value = "Could not update task"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(taskId)
            if (result.isSuccess) {
                _userMessage.value = "Task deleted"
            } else {
                _userMessage.value = "Failed to delete task"
            }
        }
    }

    companion object {
        fun provideFactory(
            repository: TaskRepository,
            userId: String
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TaskViewModel(repository, userId) as T
            }
        }
    }
}
