package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.FleetDatabase
import com.example.data.FleetRepository
import com.example.ui.screens.FleetAppMainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FleetViewModel
import com.example.ui.viewmodel.FleetViewModelFactory

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Initialize Database, Repository and ViewModel
    val database = FleetDatabase.getDatabase(applicationContext)
    val repository = FleetRepository(database.fleetDao())
    val factory = FleetViewModelFactory(application, repository)
    val viewModel = ViewModelProvider(this, factory)[FleetViewModel::class.java]

    // Pre-seed some default scenario data so the dashboards look fully populated on first launch
    viewModel.seedDemoData()

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          FleetAppMainScreen(viewModel = viewModel)
        }
      }
    }
  }
}
