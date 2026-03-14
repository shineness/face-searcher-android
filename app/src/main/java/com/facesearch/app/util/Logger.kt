package com.facesearch.app

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Logger utility for debugging and error tracking
 * Logs are saved to app's internal storage
 */
object Logger {
    private const val TAG = "FaceSearcher"
    private const val LOG_FILE = "app_error.log"
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Log an error message
     */
    fun logError(type: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val logMessage = buildString {
            append("[$timestamp] ERROR [$type]: $message")
            throwable?.let {
                append("\n  Exception: ${it.javaClass.simpleName}")
                append("\n  Message: ${it.message}")
                append("\n  StackTrace: ")
                append(it.stackTraceToString().take(500))
            }
            append("\n")
        }
        
        // Log to Logcat
        Log.e(TAG, logMessage)
        
        // Save to file
        saveToFile(logMessage)
    }
    
    /**
     * Log info message
     */
    fun logInfo(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] INFO: $message\n"
        
        Log.d(TAG, message)
        saveToFile(logMessage)
    }
    
    /**
     * Log warning
     */
    fun logWarning(message: String) {
        val timestamp = dateFormat.format(Date())
        val logMessage = "[$timestamp] WARN: $message\n"
        
        Log.w(TAG, message)
        saveToFile(logMessage)
    }
    
    /**
     * Get log file content
     */
    fun getLogContent(): String {
        return try {
            val file = File(FaceSearchApp.instance.filesDir, LOG_FILE)
            if (file.exists()) {
                file.readText()
            } else {
                "No logs yet"
            }
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }
    
    /**
     * Clear logs
     */
    fun clearLogs() {
        try {
            val file = File(FaceSearchApp.instance.filesDir, LOG_FILE)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing logs", e)
        }
    }
    
    private fun saveToFile(message: String) {
        try {
            val file = File(FaceSearchApp.instance.filesDir, LOG_FILE)
            FileWriter(file, true).use { writer ->
                writer.write(message)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving to log file", e)
        }
    }
}
