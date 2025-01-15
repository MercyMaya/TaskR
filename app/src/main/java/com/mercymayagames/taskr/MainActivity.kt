package com.mercymayagames.taskr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mercymayagames.taskr.ui.theme.TaskRTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskRTheme {
                TaskRApp()
            }
        }
    }
}

@Composable
fun TaskRApp() {
    var selectedTab by remember { mutableStateOf(MainScreenTabs.Tasks) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        when (selectedTab) {
            MainScreenTabs.Tasks -> TaskListScreen(modifier = Modifier.padding(innerPadding))
            MainScreenTabs.Completed -> CompletedTasksScreen(modifier = Modifier.padding(innerPadding))
            MainScreenTabs.Settings -> SettingsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedTab: MainScreenTabs,
    onTabSelected: (MainScreenTabs) -> Unit
) {
    NavigationBar {
        MainScreenTabs.entries.forEach { tab ->
            NavigationBarItem(
                icon = { tab.icon() },
                label = { Text(tab.title) },
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

enum class MainScreenTabs(val title: String, val icon: @Composable () -> Unit) {
    Tasks(
        title = "Tasks",
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
    ),
    Completed(
        title = "Completed",
        icon = { Icon(Icons.Filled.Check, contentDescription = null) }
    ),
    Settings(
        title = "Settings",
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
    )
}


@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Settings Screen",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun TaskRAppPreview() {
    TaskRTheme {
        TaskRApp()
    }
}