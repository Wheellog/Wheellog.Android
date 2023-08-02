package com.cooper.wheellog.settings

import androidx.annotation.StringRes
import androidx.compose.material3.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cooper.wheellog.MainActivity
import com.cooper.wheellog.R

enum class SettingsScreenEnum(@StringRes val title: Int) {
    Start(title = R.string.settings_title),
    Application(title = R.string.speed_settings_title),
    Log(title = R.string.logs_settings_title),
    Alarm(title = R.string.alarm_settings_title),
    Watch(title = R.string.watch_settings_title),
    Wheel(title = R.string.wheel_settings_title),
    Trip(title = R.string.trip_settings_title),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun settingsAppBar(
    currentScreen: SettingsScreenEnum,
    canNavigateBack: Boolean,
    navigateUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? MainActivity)
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
            } else {
                IconButton(onClick = { activity?.toggleSettings() }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = ""
                    )
                }
            }
        }
    )
}

@Composable
fun mainScreen(
    navController: NavHostController = rememberNavController()
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentScreen = SettingsScreenEnum.valueOf(
        backStackEntry?.destination?.route ?: SettingsScreenEnum.Start.name
    )
    Scaffold(
        topBar = {
            settingsAppBar(
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
                startScreen(
                    onSelect = { navController.navigate(it) }
                )
            }
            composable(route = SettingsScreenEnum.Application.name) {
                applicationScreen()
            }
            composable(route = SettingsScreenEnum.Log.name) {
                logScreen()
            }
            composable(route = SettingsScreenEnum.Alarm.name) {
                alarmScreen()
            }
            composable(route = SettingsScreenEnum.Watch.name) {
                watchScreen()
            }
            composable(route = SettingsScreenEnum.Wheel.name) {
                wheelScreen()
            }
            composable(route = SettingsScreenEnum.Trip.name) {
                tripScreen()
            }
        }
    }
}