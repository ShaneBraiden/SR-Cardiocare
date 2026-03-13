// ExerciseLibraryScreen.kt — Searchable exercise grid with category chips
package com.srcardiocare.ui.screens.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

private data class ExLibItem(
    val id: String,
    val name: String, 
    val category: String, 
    val difficulty: String, 
    val duration: String,
    val uploadedBy: String,
    val videoUrl: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(onBack: () -> Unit, onUpload: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var allExercises by remember { mutableStateOf<List<ExLibItem>>(emptyList()) }
    var categories by remember { mutableStateOf(listOf("All")) }
    var isLoading by remember { mutableStateOf(true) }

    var currentUserId by remember { mutableStateOf("") }
    var currentUserRole by remember { mutableStateOf("") }
    var showDeleteDialogFor by remember { mutableStateOf<ExLibItem?>(null) }
    var isDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val uid = FirebaseService.currentUID
            if (uid != null) {
                currentUserId = uid
                val user = FirebaseService.fetchUser(uid)
                currentUserRole = (user["role"] as? String ?: "").lowercase()
            }

            val rawExercises = FirebaseService.fetchExercises()
            allExercises = rawExercises.map { (id, data) ->
                val name = data["name"] as? String ?: ""
                val category = data["category"] as? String ?: ""
                val difficulty = (data["difficultyLevel"] as? String ?: "").replaceFirstChar { it.uppercase() }
                val durationSec = (data["durationSeconds"] as? Number)?.toInt() ?: 0
                val mins = durationSec / 60
                val secs = durationSec % 60
                val duration = if (secs > 0) "$mins:${secs.toString().padStart(2, '0')}" else "$mins:00"
                val uploadedBy = data["uploadedBy"] as? String ?: ""
                val videoUrl = data["videoUrl"] as? String
                ExLibItem(id, name, category, difficulty, duration, uploadedBy, videoUrl)
            }
            val cats = allExercises.map { it.category }.distinct().sorted()
            categories = listOf("All") + cats
        } catch (_: Exception) { }
        isLoading = false
    }

    val filtered = allExercises.filter { ex ->
        val matchesCat = selectedCategory == "All" || ex.category == selectedCategory
        val matchesSearch = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
        matchesCat && matchesSearch
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise Library", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (showDeleteDialogFor != null) {
            val ex = showDeleteDialogFor!!
            AlertDialog(
                onDismissRequest = { if (!isDeleting) showDeleteDialogFor = null },
                title = { Text("Delete Exercise", color = DesignTokens.Colors.Error) },
                text = { Text("Are you sure you want to delete '${ex.name}'? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            isDeleting = true
                            scope.launch {
                                try {
                                    FirebaseService.deleteExercise(ex.id, ex.videoUrl)
                                    allExercises = allExercises.filter { it.id != ex.id }
                                    showDeleteDialogFor = null
                                } catch (e: Exception) {
                                    showDeleteDialogFor = null
                                } finally {
                                    isDeleting = false
                                }
                            }
                        },
                        enabled = !isDeleting
                    ) {
                        if (isDeleting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = DesignTokens.Colors.Error, strokeWidth = 2.dp)
                        } else {
                            Text("Delete", color = DesignTokens.Colors.Error, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialogFor = null }, enabled = !isDeleting) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search exercises…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = DesignTokens.Spacing.XL),
                shape = RoundedCornerShape(DesignTokens.Radius.Base),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DesignTokens.Colors.Primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // Category chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.SM),
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL)
            ) {
                items(categories) { cat ->
                    val selected = cat == selectedCategory
                    FilterChip(
                        selected = selected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DesignTokens.Colors.Primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(DesignTokens.Radius.Full)
                    )
                }
            }

            Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))

            // Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.MD),
                contentPadding = PaddingValues(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered) { ex ->
                    Card(
                        shape = RoundedCornerShape(DesignTokens.Radius.LG),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(DesignTokens.Spacing.MD)) {
                            // Thumbnail placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(DesignTokens.Radius.Base))
                                    .background(DesignTokens.Colors.PrimaryLight.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🎬", style = MaterialTheme.typography.headlineMedium)

                                // Delete button for authorized users
                                val canDelete = currentUserRole == "admin" || (currentUserRole == "doctor" && ex.uploadedBy == currentUserId)
                                if (canDelete) {
                                    IconButton(
                                        onClick = { showDeleteDialogFor = ex },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = DesignTokens.Colors.Error)
                                    }
                                }

                                // Duration badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(ex.duration, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(DesignTokens.Spacing.SM))
                            Text(ex.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            Text(ex.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            // Difficulty badge
                            val badgeColor = when (ex.difficulty) {
                                "Beginner" -> DesignTokens.Colors.Success
                                "Intermediate" -> DesignTokens.Colors.Warning
                                else -> DesignTokens.Colors.Error
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                                    .background(badgeColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(ex.difficulty, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
