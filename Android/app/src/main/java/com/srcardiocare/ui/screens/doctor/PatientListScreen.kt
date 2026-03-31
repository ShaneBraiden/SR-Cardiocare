// PatientListScreen.kt — Full patient/user list view for doctors and admins
package com.srcardiocare.ui.screens.doctor

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.srcardiocare.core.security.InputValidator
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.Duration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListScreen(
    onPatientTap: (String) -> Unit,
    onDoctorTap: (String) -> Unit,
    onAddPatient: () -> Unit,
    onAddDoctor: () -> Unit,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    suspend fun loadData() {
        try {
            val uid = FirebaseService.currentUID ?: return
            val userData = FirebaseService.fetchUser(uid)
            userRole = (userData["role"] as? String)?.lowercase() ?: ""

            val users = if (userRole == "admin") {
                FirebaseService.fetchAllUsers()
            } else {
                FirebaseService.fetchPatients(uid)
            }

            allUsers = users.mapNotNull { (id, data) ->
                val role = (data["role"] as? String)?.lowercase() ?: "patient"
                if (userRole != "admin" && role != "patient") return@mapNotNull null
                if (id == uid) return@mapNotNull null

                val firstName = data["firstName"] as? String ?: ""
                val lastName = data["lastName"] as? String ?: ""
                val name = "$firstName $lastName".trim().ifBlank { data["email"] as? String ?: "Unknown" }
                val initials = "${firstName.firstOrNull() ?: ""}${lastName.firstOrNull() ?: ""}".uppercase().ifBlank { "?" }

                val lastSeenRaw = data["lastSeen"]
                val isOnline = try {
                    when (lastSeenRaw) {
                        is com.google.firebase.Timestamp -> {
                            val lastSeenInstant = Instant.ofEpochSecond(lastSeenRaw.seconds)
                            Duration.between(lastSeenInstant, Instant.now()).toMinutes() < 5
                        }
                        else -> false
                    }
                } catch (_: Exception) { false }

                val status = when (role) {
                    "admin", "doctor" -> UserStatus.ON_TRACK
                    else -> {
                        val injuries = data["injuries"] as? List<*>
                        if (injuries.isNullOrEmpty()) UserStatus.INACTIVE else UserStatus.ON_TRACK
                    }
                }

                val subtitle = when (role) {
                    "admin" -> "Administrator"
                    "doctor" -> data["speciality"] as? String ?: "Doctor"
                    else -> (data["injuries"] as? List<*>)?.firstOrNull()?.toString() ?: data["primaryGoal"] as? String ?: "Patient"
                }

                UserItem(id, name, subtitle, role, status, isOnline, initials)
            }

            errorMessage = null
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to load users"
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(Unit) { loadData() }

    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) allUsers
        else allUsers.filter { it.name.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true) }
    }

    val screenTitle = if (userRole == "admin") "All Users" else "Patients"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (userRole == "admin") {
                    ExtendedFloatingActionButton(
                        onClick = onAddDoctor,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = DesignTokens.Colors.Primary,
                        icon = { Icon(Icons.Default.Add, contentDescription = "Add Doctor") },
                        text = { Text("Add Doctor", fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                FloatingActionButton(
                    onClick = onAddPatient,
                    containerColor = DesignTokens.Colors.Primary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Patient", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch { loadData() }
            },
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = DesignTokens.Spacing.XL)
            ) {
                // Search
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = InputValidator.limitLength(it, InputValidator.MaxLength.TEXT_FIELD) },
                        placeholder = { Text(if (userRole == "admin") "Search all users…" else "Search patients…") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD),
                        shape = RoundedCornerShape(DesignTokens.Radius.Base),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DesignTokens.Colors.Primary,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }

                // Count header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (userRole == "admin") "All Users" else "Patient Overview",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "${filteredUsers.size} user${if (filteredUsers.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Loading skeleton
                if (isLoading) {
                    items(5) { SkeletonUserRow() }
                }

                // Error state
                errorMessage?.let { msg ->
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                            shape = RoundedCornerShape(DesignTokens.Radius.Card),
                            colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Error.copy(alpha = 0.08f))
                        ) {
                            Column(modifier = Modifier.padding(DesignTokens.Spacing.XL)) {
                                Text("Failed to load data", fontWeight = FontWeight.SemiBold, color = DesignTokens.Colors.Error)
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                                Text(msg, style = MaterialTheme.typography.bodySmall, color = DesignTokens.Colors.Error)
                            }
                        }
                    }
                }

                // Empty state
                if (!isLoading && errorMessage == null && filteredUsers.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.SM),
                            shape = RoundedCornerShape(DesignTokens.Radius.Card),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.XXXL),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("👥", style = MaterialTheme.typography.displaySmall)
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                                Text(
                                    if (searchQuery.isBlank()) "No users yet" else "No users match \"$searchQuery\"",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(DesignTokens.Spacing.XS))
                                Text(
                                    if (searchQuery.isBlank()) "Tap + to add your first patient" else "Try a different search term",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // User rows
                if (!isLoading) {
                    items(filteredUsers) { user ->
                        UserListRow(user = user, isAdmin = userRole == "admin", onClick = {
                            if (user.role == "patient") {
                                onPatientTap(user.id)
                            } else if (user.role == "doctor" && userRole == "admin") {
                                onDoctorTap(user.id)
                            }
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonUserRow() {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.LightGray.copy(alpha = 0.1f),
        Color.LightGray.copy(alpha = 0.3f),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim.value - 200f, 0f),
        end = Offset(translateAnim.value, 0f)
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(shimmerBrush))
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerBrush))
            }
            Box(modifier = Modifier.width(60.dp).height(22.dp).clip(RoundedCornerShape(DesignTokens.Radius.Full)).background(shimmerBrush))
        }
    }
}

@Composable
private fun UserListRow(user: UserItem, isAdmin: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.XS)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(DesignTokens.Radius.LG),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DesignTokens.Spacing.MD),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val avatarColor = when (user.role) {
                "admin"  -> DesignTokens.Colors.Warning.copy(alpha = 0.2f)
                "doctor" -> DesignTokens.Colors.Success.copy(alpha = 0.2f)
                else     -> DesignTokens.Colors.PrimaryLight
            }
            val textColor = when (user.role) {
                "admin"  -> DesignTokens.Colors.Warning
                "doctor" -> DesignTokens.Colors.Success
                else     -> DesignTokens.Colors.PrimaryDark
            }
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(user.initials, fontWeight = FontWeight.Bold, color = textColor)
                }
                if (user.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(DesignTokens.Colors.Success)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))

            Column(modifier = Modifier.weight(1f)) {
                Text(user.name, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(user.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isAdmin) {
                val (badgeText, badgeColor) = when (user.role) {
                    "admin"  -> "Admin" to DesignTokens.Colors.Warning
                    "doctor" -> "Doctor" to DesignTokens.Colors.Success
                    else     -> "Patient" to DesignTokens.Colors.Primary
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                        .background(badgeColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(badgeText, style = MaterialTheme.typography.labelSmall, color = badgeColor, fontWeight = FontWeight.SemiBold)
                }
            } else {
                val (statusText, statusColor) = when (user.status) {
                    UserStatus.ON_TRACK -> "On Track" to DesignTokens.Colors.Success
                    UserStatus.NEEDS_ATTENTION -> "Attention" to DesignTokens.Colors.Warning
                    UserStatus.INACTIVE -> "Inactive" to DesignTokens.Colors.NeutralDark
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(statusText, style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
