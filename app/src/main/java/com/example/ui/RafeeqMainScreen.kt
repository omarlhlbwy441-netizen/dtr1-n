package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.*
import com.example.ui.theme.CyberDarkBg

@Composable
fun RafeeqMainScreen(viewModel: RafeeqViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotificationMessage) {
        uiState.userNotificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberDarkBg,
        topBar = {
            Column {
                RafeeqHeader(
                    uiState = uiState,
                    onOpenAi = { viewModel.toggleAiAssistant(true) }
                )
                StatsCardsSection(uiState = uiState)
                ActionTabsBar(
                    selectedTab = uiState.selectedTab,
                    onTabSelected = { viewModel.selectTopTab(it) }
                )
            }
        },
        bottomBar = {
            RafeeqBottomBar(
                selectedIndex = uiState.selectedBottomNav,
                onSelectIndex = { navIndex ->
                    viewModel.selectBottomNav(navIndex)
                    when (navIndex) {
                        0 -> viewModel.selectTopTab(3) // Stores / Slots
                        1 -> viewModel.selectTopTab(3)
                        2 -> viewModel.selectTopTab(0) // Shorts Feed
                        3 -> viewModel.selectTopTab(1) // Live & Auctions
                        4 -> viewModel.selectTopTab(2) // Wallet
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = uiState.selectedTab,
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    0 -> ShortsFeedView(uiState = uiState, viewModel = viewModel)
                    1 -> AuctionsView(uiState = uiState, viewModel = viewModel)
                    2 -> WalletView(uiState = uiState, viewModel = viewModel)
                    3 -> VipSlotsView(uiState = uiState)
                }
            }

            if (uiState.isAiAssistantOpen) {
                AiAssistantDialog(
                    uiState = uiState,
                    onDismiss = { viewModel.toggleAiAssistant(false) },
                    onSendMessage = { prompt -> viewModel.sendAiMessage(prompt) }
                )
            }
        }
    }
}
