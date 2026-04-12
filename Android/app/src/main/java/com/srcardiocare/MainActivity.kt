package com.srcardiocare

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.srcardiocare.core.security.SecurePreferences
import com.srcardiocare.navigation.SRCardiocareNavGraph
import com.srcardiocare.navigation.Route
import com.srcardiocare.ui.theme.DesignTokens
import com.srcardiocare.ui.theme.SRCardiocareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SRCardiocareTheme {
                var startDest by remember { mutableStateOf<String?>(null) }

                // Request POST_NOTIFICATIONS permission on Android 13+
                val notifPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { /* granted or denied — notifications are optional */ }

                LaunchedEffect(Unit) {
                    val auth = (application as SRCardiocareApp).awaitAuth()
                    val prefs = SecurePreferences.getInstance(this@MainActivity)
                    val hasSeenOnboarding = prefs.getBoolean("onboarding_seen", false)

                    startDest = when {
                        !auth.isLoggedIn -> Route.Login.path
                        // Returning logged-in user who never saw onboarding
                        !hasSeenOnboarding -> Route.Onboarding.path
                        auth.userRole == "ADMIN" -> Route.AdminDashboard.path
                        auth.userRole == "DOCTOR" -> Route.DoctorDashboard.path
                        else -> Route.PatientHome.path
                    }

                    // Request notification permission after auth resolves (non-blocking)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                if (startDest == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Text(
                            text = "Powered by SRET-AIDA",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = DesignTokens.Spacing.MD)
                        )
                    }
                } else {
                    val navController = rememberNavController()
                    SRCardiocareNavGraph(
                        navController = navController,
                        startDestination = startDest!!
                    )
                }
            }
        }
    }
}
