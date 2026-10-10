package com.ngi.streamnest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ngi.streamnest.ui.StreamNestApp
import com.ngi.streamnest.ui.theme.StreamNestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StreamNestTheme {
                StreamNestApp()
            }
        }
    }
}
