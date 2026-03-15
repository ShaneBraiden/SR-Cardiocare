// DoctorDashboardScreen.kt — Doctor/Admin dashboard with pull-to-refresh and skeleton loading
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
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
import com.srcardiocare.data.firebase.FirebaseService
import com.srcardiocare.ui.theme.DesignTokens
import kotlinx.coroutines.launch

private enum class UserStatus { ON_TRACK, NEEDS_ATTENTION, INACTIVE }

private data class UserItem(
    val id: String,
    val name: String,
    val subtitle: String,
    val role: String,
    val status: UserStatus,
    val isOnline: Boolean = false,
    val initials: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorDashboardScreen(
    onPatientTap: (String) -> Unit,
    onDoctorTap: (String) -> Unit,
    onAddPatient: () -> Unit,
    onAddDoctor: () -> Unit,
    onExerciseLibrary: () -> Unit,
    onSchedule: () -> Unit,
    onVideoUpload: () -> Unit = {},
    onProfile: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var allUsers by remember { mutableStateOf<List<UserItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var doctorName by remember { mutableStateOf("") }
    var userRole by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reload trigger — incremented to trigger LaunchedEffect re-run
    var reloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // Data loading function
    suspend fun loadData() {
        try {
            val uid = FirebaseService.currentUID
            if (uid == null) {
                errorMessage = "Not signed in. Please restart the app."
                isLoading = false
                isRefreshing = false
                return
            }
            val userData = FirebaseService.fetchUser(uid)
            val firstName = userData["firstName"] as? String ?: ""
            val lastName  = userData["lastName"]  as? String ?: ""
            val role      = (userData["role"]     as? String ?: "").lowercase()
            userRole = role
            doctorName = when (role) {
                "doctor" -> "Dr. $lastName"
                "admin"  -> "$firstName $lastName (Admin)"
                else     -> "$firstName $lastName"
            }

            // Admin sees ALL users; doctors see only their assigned patients
            val users = if (role == "admin") {
                FirebaseService.fetchAllUsers()
            } else {
                FirebaseService.fetchPatients(uid)
            }

            allUsers = users.map { (id, data) ->
                val fName = data["firstName"] as? String ?: ""
                val lName = data["lastName"]  as? String ?: ""
                val userRoleStr = (data["role"] as? String ?: "patient").lowercase()
                val injuries = (data["injuries"] as? List<*>)?.firstOrNull()?.toString() ?: ""
                val initials = "${fName.firstOrNull() ?: ""}${lName.firstOrNull() ?: ""}".uppercase()

                val subtitle = when (userRoleStr) {
                    "admin"  -> "Administrator"
                    "doctor" -> data["speciality"] as? String ?: "Doctor"
                    else     -> injuries.ifBlank { "Patient" }
                }

                // Compute online status from lastSeen
                val lastSeen = data["lastSeen"]
                val isOnline = when (lastSeen) {
                    is com.google.firebase.Timestamp -> {
                        val seenMs = lastSeen.toDate().time
                        val nowMs = System.currentTimeMillis()
                        (nowMs - seenMs) < 5 * 60 * 1000
                    }
                    else -> false
                }

                UserItem(
                    id       = id,
                    name     = "$fName $lName".trim().ifBlank { "Unknown" },
                    subtitle = subtitle,
                    role     = userRoleStr,
                    status   = UserStatus.ON_TRACK,
                    isOnline = isOnline,
                    initials = initials.ifBlank { "?" }
                )
            }
            errorMessage = null
        } catch (e: Exception) {
            errorMessage = "Could not load data: ${e.message}"
        }
        isLoading = false
        isRefreshing = false
    }

    LaunchedEffect(reloadKey) {
        loadData()
    }

    val filteredUsers = if (searchQuery.isBlank()) allUsers else allUsers.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.subtitle.contains(searchQuery, ignoreCase = true) ||
        it.role.contains(searchQuery, ignoreCase = true)
    }

    // Counts for stats
    val patientCount = allUsers.count { it.role == "patient" }
    val doctorCount = allUsers.count { it.role == "doctor" }
    val onlineCount = allUsers.count { it.isOnline }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = true,
                    onClick = {
                        // Reload on tap
                        isLoading = true
                        reloadKey++
                    },
                    label = { Text("Patients") },
                    icon = { Icon(Icons.Default.People, contentDescription = "Patients") }
                )
                NavigationBarItem(selected = false, onClick = onExerciseLibrary, label = { Text("Exercises") }, icon = { Icon(Icons.Default.FitnessCenter, contentDescription = "Exercises") })
                NavigationBarItem(selected = false, onClick = onVideoUpload, label = { Text("Videos") }, icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Videos") })
                NavigationBarItem(selected = false, onClick = onSchedule, label = { Text("Schedule") }, icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Schedule") })
                NavigationBarItem(selected = false, onClick = onProfile, label = { Text("Profile") }, icon = { Icon(Icons.Default.Person, contentDescription = "Profile") })
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
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Header
                item {
                    Column(modifier = Modifier.padding(horizontal = DesignTokens.Spacing.XL, vertical = DesignTokens.Spacing.MD)) {
                        Text("Welcome back,", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(doctorName.ifBlank { "Loading…" }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }

                // Stats summary card
                if (!isLoading && errorMessage == null) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = DesignTokens.Spacing.XL),
                            shape = RoundedCornerShape(DesignTokens.Radius.Card),
                            colors = CardDefaults.cardColors(containerColor = DesignTokens.Colors.Primary)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(DesignTokens.Spacing.XL),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                if (userRole == "admin") {
                                    StatItem(value = allUsers.size.toString(), label = "Total Users")
                                    StatItem(value = patientCount.toString(), label = "Patients")
                                    StatItem(value = doctorCount.toString(), label = "Doctors")
                                    StatItem(value = onlineCount.toString(), label = "Online")
                                } else {
                                    StatItem(value = allUsers.size.toString(), label = "Total Patients")
                                    StatItem(value = allUsers.count { it.status == UserStatus.ON_TRACK }.toString(), label = "On Track")
                                    StatItem(value = onlineCount.toString(), label = "Online Now")
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(DesignTokens.Spacing.MD))
                    }
                }

                // Search
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (userRole == "admin") "Search all users…" else "Search patients…") },
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
                }

                // Section header
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

                // ── Skeleton Loading ────────────────────────────────────────
                if (isLoading) {
                    items(5) {
                        SkeletonRow()
                    }
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
                        UserRow(user = user, isAdmin = userRole == "admin", onClick = {
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

// ── Skeleton shimmer row ────────────────────────────────────────────────────
@Composable
private fun SkeletonRow() {
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
            // Avatar skeleton
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(DesignTokens.Spacing.MD))
            Column(modifier = Modifier.weight(1f)) {
                // Name skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                // Subtitle skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            // Badge skeleton
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(22.dp)
                    .clip(RoundedCornerShape(DesignTokens.Radius.Full))
                    .background(shimmerBrush)
            )
        }
    }
}

// ── User row ────────────────────────────────────────────────────────────────
@Composable
private fun UserRow(user: UserItem, isAdmin: Boolean, onClick: () -> Unit) {
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
            // Avatar with role color
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

            // Role badge (for admin view) or status badge
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

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}
