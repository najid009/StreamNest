package com.ngi.streamnest

import android.app.PictureInPictureParams
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ngi.streamnest.ui.StreamNestApp
import com.ngi.streamnest.ui.player.PlayerSession
import com.ngi.streamnest.ui.theme.StreamNestTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamNestTheme {
                StreamNestApp()
            }
        }
    }

    /** Home বাটন চাপলে লাইভ প্লেয়ার Picture-in-Picture মোডে যাবে */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PlayerSession.active && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }
    }
}
