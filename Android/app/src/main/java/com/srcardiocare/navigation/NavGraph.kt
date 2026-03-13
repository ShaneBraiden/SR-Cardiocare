// NavGraph.kt — Jetpack Compose Navigation for SR-Cardiocare
package com.srcardiocare.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.srcardiocare.ui.screens.auth.LoginScreen
import com.srcardiocare.ui.screens.onboarding.OnboardingWelcomeScreen
import com.srcardiocare.ui.screens.onboarding.OnboardingInjuryScreen
import com.srcardiocare.ui.screens.onboarding.OnboardingGoalsScreen
import com.srcardiocare.ui.screens.patient.PatientHomeScreen
import com.srcardiocare.ui.screens.patient.PatientProfileSelfScreen
import com.srcardiocare.ui.screens.workout.WorkoutPlayerScreen
import com.srcardiocare.ui.screens.feedback.PostWorkoutFeedbackScreen
import com.srcardiocare.ui.screens.doctor.DoctorDashboardScreen
import com.srcardiocare.ui.screens.doctor.AddPatientScreen
import com.srcardiocare.ui.screens.doctor.PatientProfileScreen
import com.srcardiocare.ui.screens.exercises.ExerciseLibraryScreen
import com.srcardiocare.ui.screens.video.VideoUploadScreen
import com.srcardiocare.ui.screens.schedule.ScheduleScreen
import com.srcardiocare.ui.screens.analytics.AnalyticsScreen
import com.srcardiocare.ui.screens.doctor.DoctorProfileScreen
import com.srcardiocare.ui.screens.notifications.NotificationsScreen

/**
 * Central navigation graph. All routes use sealed class [Route].
 */
sealed class Route(val path: String) {
    object Login : Route("login")
    object OnboardingWelcome : Route("onboarding/welcome")
    object OnboardingInjury : Route("onboarding/injury")
    object OnboardingGoals : Route("onboarding/goals")
    object PatientHome : Route("patient/home")
    object WorkoutPlayer : Route("workout/player")
    object PostWorkoutFeedback : Route("workout/feedback")
    object DoctorDashboard : Route("doctor/dashboard")
    object AddPatient : Route("doctor/add-patient")
    object AddDoctor : Route("doctor/add-doctor")
    object PatientProfile : Route("doctor/patient/{patientId}")
    object ExerciseLibrary : Route("exercises/library")
    object VideoUpload : Route("exercises/upload")
    object Schedule : Route("schedule")
    object Analytics : Route("analytics")
    object Notifications : Route("notifications")
    object DoctorProfile : Route("doctor/profile")
    object PatientProfileSelf : Route("patient/profile")
}

@Composable
fun SRCardiocareNavGraph(
    navController: NavHostController,
    startDestination: String = Route.Login.path
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Route.Login.path) {
            LoginScreen(
                onLoginSuccess = { role ->
                    val dest = when (role) {
                        "DOCTOR", "ADMIN" -> Route.DoctorDashboard.path
                        else -> Route.OnboardingWelcome.path
                    }
                    navController.navigate(dest) { popUpTo(Route.Login.path) { inclusive = true } }
                }
            )
        }

        composable(Route.OnboardingWelcome.path) {
            OnboardingWelcomeScreen(
                onNext = { navController.navigate(Route.OnboardingInjury.path) },
                onSkip = { navController.navigate(Route.PatientHome.path) { popUpTo(Route.OnboardingWelcome.path) { inclusive = true } } }
            )
        }

        composable(Route.OnboardingInjury.path) {
            OnboardingInjuryScreen(
                onNext = { navController.navigate(Route.OnboardingGoals.path) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.OnboardingGoals.path) {
            OnboardingGoalsScreen(
                onComplete = { navController.navigate(Route.PatientHome.path) { popUpTo(Route.OnboardingWelcome.path) { inclusive = true } } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.PatientHome.path) {
            PatientHomeScreen(
                onExerciseTap = { navController.navigate(Route.WorkoutPlayer.path) },
                onScheduleTap = { navController.navigate(Route.Schedule.path) },
                onAnalyticsTap = { navController.navigate(Route.Analytics.path) },
                onNotificationsTap = { navController.navigate(Route.Notifications.path) },
                onProfile = { navController.navigate(Route.PatientProfileSelf.path) }
            )
        }

        composable(Route.WorkoutPlayer.path) {
            WorkoutPlayerScreen(
                onFinish = { navController.navigate(Route.PostWorkoutFeedback.path) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.PostWorkoutFeedback.path) {
            PostWorkoutFeedbackScreen(
                onSubmit = { navController.navigate(Route.PatientHome.path) { popUpTo(Route.PatientHome.path) { inclusive = true } } }
            )
        }

        composable(Route.DoctorDashboard.path) {
            DoctorDashboardScreen(
                onPatientTap = { id -> navController.navigate("doctor/patient/$id") },
                onAddPatient = { navController.navigate(Route.AddPatient.path) },
                onAddDoctor = { navController.navigate(Route.AddDoctor.path) },
                onExerciseLibrary = { navController.navigate(Route.ExerciseLibrary.path) },
                onSchedule = { navController.navigate(Route.Schedule.path) },
                onVideoUpload = { navController.navigate(Route.VideoUpload.path) },
                onProfile = { navController.navigate(Route.DoctorProfile.path) }
            )
        }

        composable(Route.AddPatient.path) {
            AddPatientScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.AddDoctor.path) {
            com.srcardiocare.ui.screens.doctor.AddDoctorScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.PatientProfile.path) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            PatientProfileScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() },
                onVideoUpload = { navController.navigate(Route.VideoUpload.path) }
            )
        }

        composable(Route.ExerciseLibrary.path) {
            ExerciseLibraryScreen(
                onBack = { navController.popBackStack() },
                onUpload = { navController.navigate(Route.VideoUpload.path) }
            )
        }

        composable(Route.VideoUpload.path) {
            VideoUploadScreen(
                onBack = { navController.popBackStack() },
                onUploaded = { navController.popBackStack() }
            )
        }

        composable(Route.Schedule.path) {
            ScheduleScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Analytics.path) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.Notifications.path) {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.DoctorProfile.path) {
            DoctorProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.PatientProfileSelf.path) {
            PatientProfileSelfScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
