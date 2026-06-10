package com.suvojeet.suvmorse.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmorse.ui.screens.EncodeScreen
import com.suvojeet.suvmorse.ui.screens.ReceiveScreen

private data class MorseTab(val title: String, val icon: @Composable () -> Unit)

@Composable
fun MorseApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        MorseTab("Encode") { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null) },
        MorseTab("Receive") { Icon(Icons.Filled.Hearing, contentDescription = null) }
    )

    Scaffold(
        topBar = { BrandHeader() },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.title, fontWeight = FontWeight.SemiBold) },
                        icon = tab.icon
                    )
                }
            }
            when (selectedTab) {
                0 -> EncodeScreen(Modifier.fillMaxSize())
                else -> ReceiveScreen(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    Column(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Text(
            text = "SuvMorse",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary
        )
        Text(
            text = "− −·· · ·   encode · play · decode",
            fontSize = 13.sp,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
        )
    }
}
