package com.srcardiocare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.compose.rememberNavController
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
                // `startDest` starts null — first frame is a blank surface that
                // matches the window background, so there is no visible flash.
                var startDest by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    // Suspends off the main thread until Firebase + AuthManager are
                    // ready (usually < 200 ms, often already complete by the time
                    // the first frame is drawn).
                    val auth = (application as SRCardiocareApp).awaitAuth()
                    startDest = when {
                        !auth.isLoggedIn                              -> Route.Login.path
                        auth.userRole == "ADMIN"                       -> Route.AdminDashboard.path
                        auth.userRole == "DOCTOR"                      -> Route.DoctorDashboard.path
                        else                                          -> Route.PatientHome.path
                    }
                }

                if (startDest == null) {
                    // Invisible placeholder — matches the splash window background
                    // so there is no colour change between system splash → app.
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
