package com.suvojeet.suvmorse.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.suvmorse.ui.screens.ChartScreen
import com.suvojeet.suvmorse.ui.screens.DecodeScreen
import com.suvojeet.suvmorse.ui.screens.EncodeScreen
import com.suvojeet.suvmorse.ui.screens.ReceiveScreen
import kotlinx.coroutines.launch

private data class Destination(val title: String, val icon: ImageVector)

@Composable
fun MorseApp(
    themeMode: Int = 0,
    onThemeModeChange: (Int) -> Unit = {}
) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var showAbout by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = { msg ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(msg)
        }
    }

    val destinations = listOf(
        Destination("Encode", Icons.AutoMirrored.Filled.VolumeUp),
        Destination("Decode", Icons.Filled.Translate),
        Destination("Receive", Icons.Filled.Hearing),
        Destination("Chart", Icons.Filled.GridView)
    )

    if (showAbout) {
        AboutDialog(
            onDismiss = { showAbout = false },
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange
        )
    }

    Scaffold(
        topBar = { BrandHeader(onAbout = { showAbout = true }) },
        bottomBar = {
            NavigationBar {
                destinations.forEachIndexed { index, dest ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(dest.icon, contentDescription = dest.title) },
                        label = { Text(dest.title) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            Crossfade(targetState = selected, animationSpec = tween(250), label = "tab") { tab ->
                when (tab) {
                    0 -> EncodeScreen(Modifier.fillMaxSize(), showMessage)
                    1 -> DecodeScreen(Modifier.fillMaxSize(), showMessage)
                    2 -> ReceiveScreen(Modifier.fillMaxSize(), showMessage)
                    else -> ChartScreen(Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun BrandHeader(onAbout: () -> Unit) {
    val gradient = Brush.horizontalGradient(
        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
    )
    val onGradient = MaterialTheme.colorScheme.onPrimary
    Row(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .statusBarsPadding()
            .padding(start = 20.dp, top = 14.dp, end = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(onGradient.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "·−",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = onGradient
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = "SuvMorse",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = onGradient
            )
            Text(
                text = "encode · play · decode",
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                color = onGradient.copy(alpha = 0.85f)
            )
        }
        IconButton(onClick = onAbout) {
            Icon(Icons.Outlined.Info, contentDescription = "About", tint = onGradient)
        }
    }
}
