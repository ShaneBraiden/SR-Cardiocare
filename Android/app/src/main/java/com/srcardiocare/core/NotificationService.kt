// NotificationService.kt — Workout reminder notifications and activity alerts
package com.srcardiocare.core

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.srcardiocare.R
import java.util.Calendar

/**
 * Notification service for workout reminders and activity alerts.
 * Schedules notifications at 30min, 1hr, 2hr before workout time.
 */
object NotificationService {
    
    private const val CHANNEL_WORKOUT_REMINDER = "workout_reminders"
    private const val CHANNEL_ACTIVITY = "activity_updates"
    private const val CHANNEL_POPUP = "in_app_popups"
    
    private const val NOTIFICATION_ID_REMINDER_30 = 1001
    private const val NOTIFICATION_ID_REMINDER_60 = 1002
    private const val NOTIFICATION_ID_REMINDER_120 = 1003
    private const val NOTIFICATION_ID_ACTIVITY_BASE = 2000
    
    // Reminder intervals in minutes
    val REMINDER_INTERVALS = listOf(30, 60, 120)
    
    /**
     * Initialize notification channels (call on app startup)
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Workout reminders channel
            val reminderChannel = NotificationChannel(
                CHANNEL_WORKOUT_REMINDER,
                "Workout Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for upcoming workouts"
                enableVibration(true)
            }
            
            // Activity updates channel
            val activityChannel = NotificationChannel(
                CHANNEL_ACTIVITY,
                "Activity Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates about doctor and patient activities"
            }
            
            // In-app popup channel
            val popupChannel = NotificationChannel(
                CHANNEL_POPUP,
                "In-App Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important in-app alerts"
            }
            
            manager.createNotificationChannels(listOf(reminderChannel, activityChannel, popupChannel))
        }
    }
    
    /**
     * Schedule workout reminder notifications.
     * @param context Application context
     * @param workoutTimeMs The scheduled workout time in milliseconds
     * @param workoutName Name of the workout
     */
    fun scheduleWorkoutReminders(
        context: Context,
        workoutTimeMs: Long,
        workoutName: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        REMINDER_INTERVALS.forEachIndexed { index, minutesBefore ->
            val triggerTime = workoutTimeMs - (minutesBefore * 60 * 1000)
            
            // Don't schedule if time has already passed
            if (triggerTime <= System.currentTimeMillis()) return@forEachIndexed
            
            val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                putExtra("workout_name", workoutName)
                putExtra("minutes_before", minutesBefore)
                putExtra("notification_id", NOTIFICATION_ID_REMINDER_30 + index)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_REMINDER_30 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                // Fallback to inexact alarm
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }
    
    /**
     * Cancel all scheduled workout reminders
     */
    fun cancelWorkoutReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        REMINDER_INTERVALS.forEachIndexed { index, _ ->
            val intent = Intent(context, WorkoutReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                NOTIFICATION_ID_REMINDER_30 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
    
    /**
     * Show an immediate notification for activity updates
     */
    fun showActivityNotification(
        context: Context,
        title: String,
        message: String,
        activityId: String
    ) {
        if (!hasNotificationPermission(context)) return
        
        val notificationId = NOTIFICATION_ID_ACTIVITY_BASE + activityId.hashCode()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Show workout reminder notification
     */
    fun showWorkoutReminderNotification(
        context: Context,
        workoutName: String,
        minutesBefore: Int,
        notificationId: Int
    ) {
        if (!hasNotificationPermission(context)) return
        
        val timeText = when (minutesBefore) {
            0 -> "now"
            30 -> "30 minutes"
            60 -> "1 hour"
            120 -> "2 hours"
            else -> "$minutesBefore minutes"
        }
        
        val contentText = if (minutesBefore == 0) {
            "Don't miss your workout today!"
        } else {
            "$workoutName starts in $timeText"
        }
        
        val notification = NotificationCompat.Builder(context, CHANNEL_WORKOUT_REMINDER)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Workout Reminder")
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }
    
    /**
     * Schedule daily workout reminder at specified time
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            
            // If time has passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        
        val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
            putExtra("is_daily_reminder", true)
            putExtra("notification_id", 3001)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Handle permission issues
        }
    }
    
    /**
     * Update notification badge count
     */
    fun updateBadgeCount(context: Context, count: Int) {
        // Android handles badge counts differently per launcher
        // This is a simplified implementation
        try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE")
            intent.putExtra("badge_count", count)
            intent.putExtra("badge_count_package_name", context.packageName)
            intent.putExtra("badge_count_class_name", "${context.packageName}.MainActivity")
            context.sendBroadcast(intent)
        } catch (_: Exception) {
            // Not all launchers support this
        }
    }
    
    /**
     * Clear all notifications
     */
    fun clearAllNotifications(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancelAll()
    }
    
    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

/**
 * Broadcast receiver for workout reminder alarms
 */
class WorkoutReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isDailyReminder = intent.getBooleanExtra("is_daily_reminder", false)
        
        if (isDailyReminder) {
            // Show generic daily reminder
            NotificationService.showWorkoutReminderNotification(
                context = context,
                workoutName = "Your workout",
                minutesBefore = 0,
                notificationId = intent.getIntExtra("notification_id", 3001)
            )
        } else {
            val workoutName = intent.getStringExtra("workout_name") ?: "Workout"
            val minutesBefore = intent.getIntExtra("minutes_before", 30)
            val notificationId = intent.getIntExtra("notification_id", 1001)
            
            NotificationService.showWorkoutReminderNotification(
                context = context,
                workoutName = workoutName,
                minutesBefore = minutesBefore,
                notificationId = notificationId
            )
        }
    }
}
