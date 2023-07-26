package com.cooper.wheellog.preferences

import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cooper.wheellog.R

enum class SettingsScreenEnum(@StringRes val title: Int) {
    Start(title = R.string.settings_title),
    Application(title = R.string.speed_settings_title),
    Log(title = R.string.logs_settings_title),
    Alarm(title = R.string.alarm_preferences),
    Watch(title = R.string.watch_settings_title),
    Wheel(title = R.string.wheel_settings_title),
    Trip(title = R.string.trip_settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppBar(
    currentScreen: SettingsScreenEnum,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        title = { Text(stringResource(currentScreen.title)) },
        colors = TopAppBarDefaults.mediumTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = modifier,
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = navigateUp) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = ""
                    )
                }
            }
        }
    )
}

@Composable
fun SettingsMainScreen(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = SettingsScreenEnum.valueOf(
        backStackEntry?.destination?.route ?: SettingsScreenEnum.Start.name
    )
    Scaffold(
        topBar = {
            SettingsAppBar(
                currentScreen = currentScreen,
                canNavigateBack = navController.previousBackStackEntry != null,
                navigateUp = { navController.navigateUp() }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SettingsScreenEnum.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = SettingsScreenEnum.Start.name) {
                StartScreen(
                    onSelect = { navController.navigate(it) }
                )
            }
            composable(route = SettingsScreenEnum.Application.name) {
                ApplicationScreen()
            }
        }
    }
}