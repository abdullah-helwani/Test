package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Routine
import com.example.data.model.Task
import com.example.data.repository.RoutineRepository
import com.example.data.repository.TaskRepository
import com.example.ui.viewmodel.RoutineViewModel
import com.example.ui.viewmodel.TaskViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class MainTab {
    TODAY, TASKS, ROUTINES, ANALYTICS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentUserId: String,
    onSignOutClicked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val taskRepository = remember { TaskRepository(context) }
    val routineRepository = remember { RoutineRepository(context) }

    val taskViewModel: TaskViewModel = viewModel(
        key = "task_vm_$currentUserId",
        factory = TaskViewModel.provideFactory(taskRepository, currentUserId)
    )
    val routineViewModel: RoutineViewModel = viewModel(
        key = "routine_vm_$currentUserId",
        factory = RoutineViewModel.provideFactory(routineRepository, currentUserId)
    )

    val taskUiState by taskViewModel.uiState.collectAsStateWithLifecycle()
    val routineUiState by routineViewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.TODAY) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // Bottom sheet states
    val taskSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTaskSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }

    val routineSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRoutineSheet by remember { mutableStateOf(false) }
    var routineToEdit by remember { mutableStateOf<Routine?>(null) }

    // Surface messages
    LaunchedEffect(taskUiState.userMessage) {
        taskUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            taskViewModel.clearUserMessage()
        }
    }
    LaunchedEffect(routineUiState.userMessage) {
        routineUiState.userMessage?.let {
            snackbarHostState.showSnackbar(it)
            routineViewModel.clearUserMessage()
        }
    }

    val user = Firebase.auth.currentUser

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (currentTab) {
                                MainTab.TODAY -> "Today"
                                MainTab.TASKS -> "My Tasks"
                                MainTab.ROUTINES -> "Routines"
                                MainTab.ANALYTICS -> "Analytics"
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showProfileMenu = true },
                            modifier = Modifier.testTag("profile_menu_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "User profile",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = user?.displayName ?: "Signed In User",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = user?.email ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = { }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Sign out",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showProfileMenu = false
                                    onSignOutClicked()
                                },
                                modifier = Modifier.testTag("sign_out_menu_item")
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.TODAY,
                    onClick = { currentTab = MainTab.TODAY },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today") },
                    modifier = Modifier.testTag("tab_today")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.TASKS,
                    onClick = { currentTab = MainTab.TASKS },
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = "Tasks") },
                    label = { Text("Tasks") },
                    modifier = Modifier.testTag("tab_tasks")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ROUTINES,
                    onClick = { currentTab = MainTab.ROUTINES },
                    icon = { Icon(Icons.Default.Loop, contentDescription = "Routines") },
                    label = { Text("Routines") },
                    modifier = Modifier.testTag("tab_routines")
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.ANALYTICS,
                    onClick = { currentTab = MainTab.ANALYTICS },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = "Analytics") },
                    label = { Text("Insights") },
                    modifier = Modifier.testTag("tab_analytics")
                )
            }
        },
        floatingActionButton = {
            if (currentTab != MainTab.ANALYTICS) {
                FloatingActionButton(
                    onClick = {
                        if (currentTab == MainTab.ROUTINES) {
                            routineToEdit = null
                            showRoutineSheet = true
                        } else {
                            taskToEdit = null
                            showTaskSheet = true
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("main_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (currentTab == MainTab.ROUTINES) "Add Routine" else "Add Task"
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                MainTab.TODAY -> TodayScreen(
                    tasks = taskUiState.tasks,
                    routines = routineUiState.routines,
                    onToggleTask = { taskViewModel.toggleTaskCompletion(it) },
                    onStartRoutine = { routineViewModel.startRoutine(it) },
                    onAddTaskClick = {
                        taskToEdit = null
                        showTaskSheet = true
                    },
                    onAddRoutineClick = {
                        routineToEdit = null
                        showRoutineSheet = true
                    }
                )

                MainTab.TASKS -> TasksScreen(
                    tasks = taskUiState.filteredTasks,
                    categories = taskUiState.categories,
                    selectedCategory = taskUiState.selectedCategory,
                    filterStatus = taskUiState.filterStatus,
                    sortOption = taskUiState.sortOption,
                    searchQuery = taskUiState.searchQuery,
                    onCategorySelected = { taskViewModel.setCategory(it) },
                    onFilterStatusSelected = { taskViewModel.setFilterStatus(it) },
                    onSortOptionSelected = { taskViewModel.setSortOption(it) },
                    onSearchQueryChanged = { taskViewModel.setSearchQuery(it) },
                    onToggleTask = { taskViewModel.toggleTaskCompletion(it) },
                    onEditTask = {
                        taskToEdit = it
                        showTaskSheet = true
                    },
                    onDeleteTask = { taskViewModel.deleteTask(it) }
                )

                MainTab.ROUTINES -> RoutinesScreen(
                    routines = routineUiState.routines,
                    selectedTimeOfDay = routineUiState.selectedTimeOfDay,
                    onTimeOfDaySelected = { routineViewModel.setTimeOfDayFilter(it) },
                    onStartRoutine = { routineViewModel.startRoutine(it) },
                    onEditRoutine = {
                        routineToEdit = it
                        showRoutineSheet = true
                    },
                    onDeleteRoutine = { routineViewModel.deleteRoutine(it) }
                )

                MainTab.ANALYTICS -> AnalyticsScreen(
                    tasks = taskUiState.tasks,
                    routines = routineUiState.routines,
                    routineLogs = routineUiState.routineLogs
                )
            }
        }
    }

    // Task Creation / Editing Sheet
    if (showTaskSheet) {
        TaskBottomSheet(
            sheetState = taskSheetState,
            initialTask = taskToEdit,
            onDismiss = { showTaskSheet = false },
            onSave = { title, desc, priority, category, dueDate, dueTime, subtasks ->
                if (taskToEdit == null) {
                    taskViewModel.createTask(title, desc, priority, category, dueDate, dueTime, subtasks)
                } else {
                    taskViewModel.updateTask(
                        taskToEdit!!.copy(
                            title = title,
                            description = desc,
                            priority = priority,
                            category = category,
                            dueDate = dueDate,
                            dueTime = dueTime,
                            subtasks = subtasks
                        )
                    )
                }
                showTaskSheet = false
            }
        )
    }

    // Routine Creation / Editing Sheet
    if (showRoutineSheet) {
        RoutineBottomSheet(
            sheetState = routineSheetState,
            initialRoutine = routineToEdit,
            onDismiss = { showRoutineSheet = false },
            onSave = { title, desc, timeOfDay, targetTime, days, steps ->
                if (routineToEdit == null) {
                    routineViewModel.createRoutine(title, desc, timeOfDay, targetTime, days, steps)
                } else {
                    routineViewModel.updateRoutine(
                        routineToEdit!!.copy(
                            title = title,
                            description = desc,
                            timeOfDay = timeOfDay,
                            targetTime = targetTime,
                            daysOfWeek = days,
                            steps = steps
                        )
                    )
                }
                showRoutineSheet = false
            }
        )
    }

    // Active Routine Interactive Player Dialog
    routineUiState.activePlayingRoutine?.let { playingRoutine ->
        RoutinePlayerDialog(
            routine = playingRoutine,
            currentStepIndex = routineUiState.playingStepIndex,
            completedSteps = routineUiState.completedStepsSet,
            isTimerRunning = routineUiState.isTimerRunning,
            elapsedSeconds = routineUiState.elapsedSeconds,
            onToggleTimer = { routineViewModel.toggleTimer() },
            onToggleStep = { routineViewModel.toggleCurrentStepCompletion(it) },
            onNextStep = { routineViewModel.nextStep() },
            onPrevStep = { routineViewModel.prevStep() },
            onFinish = { routineViewModel.finishRoutine() },
            onCancel = { routineViewModel.cancelPlayingRoutine() }
        )
    }

    // Celebration Dialog when routine finishes
    if (routineUiState.showCelebrationDialog) {
        CelebrationDialog(
            streakCount = routineUiState.celebrationStreak,
            onDismiss = { routineViewModel.dismissCelebration() }
        )
    }
}
