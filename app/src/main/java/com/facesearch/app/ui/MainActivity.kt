package com.facesearch.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.facesearch.app.util.Logger

/**
 * Main Activity - Entry point of the app
 * Wraps the Compose UI with error handling
 */
class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Logger.logInfo("MainActivity.onCreate")
        
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                MainScreen()
            }
        }
        
        Logger.logInfo("MainActivity created successfully")
    }
    
    override fun onResume() {
        super.onResume()
        Logger.logInfo("MainActivity.onResume")
    }
    
    override fun onPause() {
        super.onPause()
        Logger.logInfo("MainActivity.onPause")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Logger.logInfo("MainActivity.onDestroy")
    }
}
