package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Routine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineBottomSheet(
    sheetState: SheetState,
    initialRoutine: Routine? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String?, timeOfDay: String, targetTime: String?, days: List<String>, steps: List<String>) -> Unit
) {
    var title by remember { mutableStateOf(initialRoutine?.title ?: "") }
    var description by remember { mutableStateOf(initialRoutine?.description ?: "") }
    var timeOfDay by remember { mutableStateOf(initialRoutine?.timeOfDay ?: "morning") }
    var targetTime by remember { mutableStateOf(initialRoutine?.targetTime ?: "07:30") }

    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val selectedDays = remember {
        mutableStateListOf<String>().apply {
            if (initialRoutine != null && initialRoutine.daysOfWeek.isNotEmpty()) {
                addAll(initialRoutine.daysOfWeek)
            } else {
                addAll(listOf("MON", "TUE", "WED", "THU", "FRI"))
            }
        }
    }

    val steps = remember {
        mutableStateListOf<String>().apply {
            if (initialRoutine != null && initialRoutine.steps.isNotEmpty()) {
                addAll(initialRoutine.steps)
            } else {
                addAll(listOf("Drink a glass of water", "5-minute stretch or movement", "Review top tasks for the day"))
            }
        }
    }

    var newStepText by remember { mutableStateOf("") }
    val timeOptions = listOf("morning", "afternoon", "evening", "anytime")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (initialRoutine == null) "New Routine" else "Edit Routine",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Routine Name *") },
                placeholder = { Text("e.g. Morning Miracle, Night Wind-Down") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("routine_title_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                placeholder = { Text("e.g. Set up your day for peak focus") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Time of Day
            Text(
                text = "Time of Day",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeOptions.forEach { opt ->
                    val isSelected = timeOfDay.equals(opt, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { timeOfDay = opt },
                        label = { Text(opt.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = targetTime,
                onValueChange = { targetTime = it },
                label = { Text("Target Time (HH:mm)") },
                placeholder = { Text("07:30") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Days of the week
            Text(
                text = "Active Days",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allDays.forEach { day ->
                    val isSelected = selectedDays.contains(day)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (isSelected) {
                                    if (selectedDays.size > 1) selectedDays.remove(day)
                                } else {
                                    selectedDays.add(day)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.take(1),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Steps Builder
            Text(
                text = "Routine Steps (${steps.size}) *",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            steps.forEachIndexed { index, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { steps.removeAt(index) },
                        modifier = Modifier.size(32.dp),
                        enabled = steps.size > 1
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove step",
                            modifier = Modifier.size(16.dp),
                            tint = if (steps.size > 1) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newStepText,
                    onValueChange = { newStepText = it },
                    placeholder = { Text("Add next routine action...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (newStepText.isNotBlank()) {
                            steps.add(newStepText.trim())
                            newStepText = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add step",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && steps.isNotEmpty()) {
                        onSave(
                            title,
                            description.takeIf { it.isNotBlank() },
                            timeOfDay,
                            targetTime.takeIf { it.isNotBlank() },
                            selectedDays.toList(),
                            steps.toList()
                        )
                    }
                },
                enabled = title.isNotBlank() && steps.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_routine_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (initialRoutine == null) "Create Routine" else "Save Routine")
            }
        }
    }
}
