package com.srcardiocare

import android.app.Application
import com.google.firebase.FirebaseApp
import com.srcardiocare.core.auth.AuthManager
import com.srcardiocare.core.NotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async

/**
 * SR-Cardiocare Application class.
 *
 * Firebase and AuthManager are initialised on a background thread so the main
 * thread is never blocked during startup.  MainActivity suspends via [awaitAuth]
 * inside a LaunchedEffect — the first Compose frame renders immediately.
 */
class SRCardiocareApp : Application() {

    /** App-wide scope that survives configuration changes. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Deferred AuthManager — resolved on a background thread.
     * Callers should use [awaitAuth] rather than touching this directly.
     */
    private val authDeferred = appScope.async(Dispatchers.IO) {
        // FirebaseApp.initializeApp performs disk I/O (reads google-services.json).
        // Keeping it off the main thread removes the biggest startup block.
        FirebaseApp.initializeApp(this@SRCardiocareApp)
        AuthManager(this@SRCardiocareApp)
    }

    /**
     * Suspends until [AuthManager] is ready.  Safe to call from any coroutine;
     * subsequent calls return the already-resolved instance instantly.
     */
    suspend fun awaitAuth(): AuthManager = authDeferred.await()

    companion object {
        lateinit var instance: SRCardiocareApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize notification channels
        NotificationService.createChannels(this)
        
        // Kick off async init immediately — do NOT block here.
        // authDeferred is already started by the field initialiser above.
    }
}
