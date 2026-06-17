package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MyNotificationAppView
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MyNotificationViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge full screen drawing to support M3 guidelines
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Main layout surface container
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val viewModel: MyNotificationViewModel = viewModel()
                    MyNotificationAppView(viewModel = viewModel)
                }
            }
        }
    }
}
