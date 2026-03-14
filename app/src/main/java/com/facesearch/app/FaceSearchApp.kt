package com.facesearch.app

import android.app.Application
import android.util.Log

/**
 * Application class with global error handling
 */
class FaceSearchApp : Application() {
    
    companion object {
        const val TAG = "FaceSearchApp"
        lateinit var instance: FaceSearchApp
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Set up global exception handler
        setupErrorHandling()
        
        Log.d(TAG, "Application started")
    }
    
    private fun setupErrorHandling() {
        // Handle uncaught exceptions in threads
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            // Log to file for debugging
            Logger.logError("UncaughtException", throwable.message ?: "Unknown error", throwable)
        }
    }
}
