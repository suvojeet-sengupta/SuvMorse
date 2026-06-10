package com.suvojeet.suvmorse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.suvojeet.suvmorse.ui.MorseApp
import com.suvojeet.suvmorse.ui.theme.SuvMorseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuvMorseTheme {
                MorseApp()
            }
        }
    }
}
