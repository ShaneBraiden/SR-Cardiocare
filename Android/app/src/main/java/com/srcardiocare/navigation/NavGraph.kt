// NavGraph.kt — Jetpack Compose Navigation for SR-Cardiocare
package com.srcardiocare.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.srcardiocare.ui.screens.auth.LoginScreen
import com.srcardiocare.ui.screens.auth.ChangePasswordScreen
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
    object PatientHome : Route("patient/home")
    object WorkoutPlayer : Route("workout/player?name={name}&videoUrl={videoUrl}&sets={sets}&reps={reps}&instructions={instructions}&planId={planId}&totalCount={totalCount}&isLastExercise={isLastExercise}") {
        fun createPath(
            name: String,
            videoUrl: String?,
            sets: Int,
            reps: Int,
            instructions: String?,
            planId: String,
            totalCount: Int,
            isLastExercise: Boolean
        ): String {
            val encodedName = Uri.encode(name)
            val encodedVideoUrl = Uri.encode(videoUrl ?: "")
            val encodedInstructions = Uri.encode(instructions ?: "")
            val encodedPlanId = Uri.encode(planId)
            return "workout/player?name=$encodedName&videoUrl=$encodedVideoUrl&sets=$sets&reps=$reps&instructions=$encodedInstructions&planId=$encodedPlanId&totalCount=$totalCount&isLastExercise=$isLastExercise"
        }
    }
    object PostWorkoutFeedback : Route("workout/feedback")
    object DoctorDashboard : Route("doctor/dashboard")
    object FeedbackDashboard : Route("doctor/feedback")
    object AddPatient : Route("doctor/add-patient")
    object AddDoctor : Route("doctor/add-doctor")
    object PatientProfile : Route("doctor/patient/{patientId}")
    object AdminDoctorProfile : Route("admin/doctor/{doctorId}")
    object ExerciseLibrary : Route("exercises/library")
    object VideoUpload : Route("exercises/upload")
    object Schedule : Route("schedule")
    object Analytics : Route("analytics")
    object Notifications : Route("notifications")
    object DoctorProfile : Route("doctor/profile")
    object PatientProfileSelf : Route("patient/profile")
    object PatientChat : Route("patient/chat")
    object ChangePassword : Route("change-password")
    object PatientFeedbackChat : Route("doctor/patientChat/{patientId}") {
        fun createPath(patientId: String) = "doctor/patientChat/$patientId"
    }
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
                        else -> Route.PatientHome.path // Skip onboarding, go directly to home
                    }
                    navController.navigate(dest) { popUpTo(Route.Login.path) { inclusive = true } }
                },
                onChangePassword = { navController.navigate(Route.ChangePassword.path) }
            )
        }

        composable(Route.PatientHome.path) {
            PatientHomeScreen(
                onExerciseTap = { name, videoUrl, sets, reps, instructions, planId, totalCount, isLastExercise ->
                    navController.navigate(
                        Route.WorkoutPlayer.createPath(
                            name = name,
                            videoUrl = videoUrl,
                            sets = sets,
                            reps = reps,
                            instructions = instructions,
                            planId = planId,
                            totalCount = totalCount,
                            isLastExercise = isLastExercise
                        )
                    )
                },
                onScheduleTap = { navController.navigate(Route.Schedule.path) },
                onAnalyticsTap = { navController.navigate(Route.Analytics.path) },
                onNotificationsTap = { navController.navigate(Route.Notifications.path) },
                onChatTap = { navController.navigate(Route.PatientChat.path) },
                onProfile = { navController.navigate(Route.PatientProfileSelf.path) }
            )
        }

        composable(Route.PatientChat.path) {
            com.srcardiocare.ui.screens.patient.PatientChatScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.WorkoutPlayer.path,
            arguments = listOf(
                navArgument("name") { type = NavType.StringType; defaultValue = "Workout" },
                navArgument("videoUrl") { type = NavType.StringType; defaultValue = "" },
                navArgument("sets") { type = NavType.IntType; defaultValue = 3 },
                navArgument("reps") { type = NavType.IntType; defaultValue = 10 },
                navArgument("instructions") { type = NavType.StringType; defaultValue = "" },
                navArgument("planId") { type = NavType.StringType; defaultValue = "" },
                navArgument("totalCount") { type = NavType.IntType; defaultValue = 1 },
                navArgument("isLastExercise") { type = NavType.BoolType; defaultValue = false }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments
            WorkoutPlayerScreen(
                exerciseName = args?.getString("name") ?: "Workout",
                videoUrl = args?.getString("videoUrl").orEmpty().ifBlank { null },
                sets = args?.getInt("sets") ?: 3,
                reps = args?.getInt("reps") ?: 10,
                instructions = args?.getString("instructions").orEmpty().ifBlank { null },
                planId = args?.getString("planId") ?: "",
                totalCount = args?.getInt("totalCount") ?: 1,
                isLastExercise = args?.getBoolean("isLastExercise") ?: false,
                onFinish = {
                    if (args?.getBoolean("isLastExercise") == true) {
                        navController.navigate(Route.PostWorkoutFeedback.path) {
                            popUpTo(Route.PatientHome.path)
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
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
                onDoctorTap = { id -> navController.navigate("admin/doctor/$id") },
                onAddPatient = { navController.navigate(Route.AddPatient.path) },
                onAddDoctor = { navController.navigate(Route.AddDoctor.path) },
                onExerciseLibrary = { navController.navigate(Route.ExerciseLibrary.path) },
                onSchedule = { navController.navigate(Route.Schedule.path) },
                onProfile = { navController.navigate(Route.DoctorProfile.path) },
                onFeedbacks = { navController.navigate(Route.FeedbackDashboard.path) }
            )
        }

        composable(Route.FeedbackDashboard.path) {
            com.srcardiocare.ui.screens.doctor.FeedbackDashboardScreen(
                onPatientTap = { id -> navController.navigate(Route.PatientFeedbackChat.createPath(id)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.PatientFeedbackChat.path,
            arguments = listOf(navArgument("patientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val patientId = backStackEntry.arguments?.getString("patientId") ?: ""
            com.srcardiocare.ui.screens.doctor.PatientFeedbackChatScreen(
                patientId = patientId,
                onBack = { navController.popBackStack() }
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

        composable(Route.AdminDoctorProfile.path) { backStackEntry ->
            val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
            com.srcardiocare.ui.screens.doctor.AdminDoctorProfileScreen(
                doctorId = doctorId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() }
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
                },
                onChangePassword = { navController.navigate(Route.ChangePassword.path) }
            )
        }

        composable(Route.PatientProfileSelf.path) {
            PatientProfileSelfScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Route.Login.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangePassword = { navController.navigate(Route.ChangePassword.path) }
            )
        }

        composable(Route.ChangePassword.path) {
            ChangePasswordScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() }
            )
        }
    }
}
