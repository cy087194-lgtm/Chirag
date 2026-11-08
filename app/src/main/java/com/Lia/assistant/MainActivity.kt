package com.Lia.assistant

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.Lia.assistant.data.NovaAppState
import com.Lia.assistant.forge.ForgeController
import com.Lia.assistant.license.AccessKeyManager
import com.Lia.assistant.ui.fx.ListeningEdgeGlow
import com.Lia.assistant.ui.navigation.NovaApp
import com.Lia.assistant.ui.screens.splash.LiaSplash3D
import com.Lia.assistant.ui.screens.splash.SplashSession
import com.Lia.assistant.ui.theme.NovaTheme
import com.Lia.assistant.voice.VoiceForegroundService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var appState: NovaAppState

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        appState = NovaAppState(this)
        handleIntent(intent)

        setContent {
            var showSplash by remember { mutableStateOf(!SplashSession.played) }

            NovaTheme(
                mode = appState.themeMode,
                reducedMotion = appState.reducedMotion
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    NovaApp(appState = appState)

                    if (showSplash) {
                        LiaSplash3D(onDismiss = { showSplash = false })
                    }

                    ListeningEdgeGlow()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra("EXTRA_OPEN_FORGE", false) == true) {
            ForgeController.requestOpen()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val isAllowed = AccessKeyManager.liveCheck(this@MainActivity)
            if (!isAllowed) {
                VoiceForegroundService.stop(this@MainActivity)
            }
        }
    }
}
