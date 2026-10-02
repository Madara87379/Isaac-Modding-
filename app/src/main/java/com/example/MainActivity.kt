package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.AppTab
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.components.IsaacHeader
import com.example.ui.screens.*
import com.example.ui.theme.IsaacBackground
import com.example.ui.theme.IsaacBorder
import com.example.ui.theme.IsaacSurface
import com.example.ui.theme.IsaacTearBlue
import com.example.ui.theme.IsaacTextMuted
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val layoutDirection = if (uiState.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                    MainScreen(
                        uiState = uiState,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = IsaacBackground,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            IsaacHeader(
                isArabic = uiState.isArabic,
                selectedModel = uiState.selectedModel,
                onToggleLanguage = { viewModel.toggleLanguage() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = IsaacSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.CONVERTER,
                    onClick = { viewModel.setTab(AppTab.CONVERTER) },
                    icon = { Icon(Icons.Default.Transform, contentDescription = "Converter") },
                    label = { Text(if (uiState.isArabic) "المحوّل" else "Converter") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IsaacSurface,
                        selectedTextColor = IsaacTearBlue,
                        indicatorColor = IsaacTearBlue,
                        unselectedIconColor = IsaacTextMuted,
                        unselectedTextColor = IsaacTextMuted
                    ),
                    modifier = Modifier.testTag("tab_converter")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.ASSISTANT,
                    onClick = { viewModel.setTab(AppTab.ASSISTANT) },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI Chat") },
                    label = { Text(if (uiState.isArabic) "المساعد" else "AI Chat") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IsaacSurface,
                        selectedTextColor = IsaacTearBlue,
                        indicatorColor = IsaacTearBlue,
                        unselectedIconColor = IsaacTextMuted,
                        unselectedTextColor = IsaacTextMuted
                    ),
                    modifier = Modifier.testTag("tab_assistant")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.GUIDE,
                    onClick = { viewModel.setTab(AppTab.GUIDE) },
                    icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Guide") },
                    label = { Text(if (uiState.isArabic) "الدليل" else "Guide") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IsaacSurface,
                        selectedTextColor = IsaacTearBlue,
                        indicatorColor = IsaacTearBlue,
                        unselectedIconColor = IsaacTextMuted,
                        unselectedTextColor = IsaacTextMuted
                    ),
                    modifier = Modifier.testTag("tab_guide")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.HISTORY,
                    onClick = { viewModel.setTab(AppTab.HISTORY) },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text(if (uiState.isArabic) "السجل" else "History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IsaacSurface,
                        selectedTextColor = IsaacTearBlue,
                        indicatorColor = IsaacTearBlue,
                        unselectedIconColor = IsaacTextMuted,
                        unselectedTextColor = IsaacTextMuted
                    ),
                    modifier = Modifier.testTag("tab_history")
                )

                NavigationBarItem(
                    selected = uiState.currentTab == AppTab.SETTINGS,
                    onClick = { viewModel.setTab(AppTab.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(if (uiState.isArabic) "الإعدادات" else "Settings") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = IsaacSurface,
                        selectedTextColor = IsaacTearBlue,
                        indicatorColor = IsaacTearBlue,
                        unselectedIconColor = IsaacTextMuted,
                        unselectedTextColor = IsaacTextMuted
                    ),
                    modifier = Modifier.testTag("tab_settings")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(IsaacBackground)
        ) {
            when (uiState.currentTab) {
                AppTab.CONVERTER -> ConverterScreen(uiState = uiState, viewModel = viewModel)
                AppTab.ASSISTANT -> AssistantScreen(uiState = uiState, viewModel = viewModel)
                AppTab.GUIDE -> GuideScreen(uiState = uiState)
                AppTab.HISTORY -> HistoryScreen(uiState = uiState, viewModel = viewModel)
                AppTab.SETTINGS -> SettingsScreen(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}
