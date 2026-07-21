package com.example.smartplannerapp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.smartplannerapp.data.DataRepository
import com.example.smartplannerapp.ui.main.MainScreen

@Composable
fun MainNavigation(repository: DataRepository? = null) {
    val backStack = rememberNavBackStack(Main)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<Main> {
                MainScreen(
                    onItemClick = { navKey -> backStack.add(navKey) },
                    modifier = Modifier.fillMaxSize(),
                    repository = repository
                )
            }
        }
    )
}
