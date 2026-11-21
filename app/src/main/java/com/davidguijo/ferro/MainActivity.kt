package com.davidguijo.ferro

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.feature.workout.ActiveWorkoutManager
import com.davidguijo.ferro.navigation.FerroNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * Única Activity de la app. Extiende AppCompatActivity para que el idioma por app
 * (AppCompatDelegate.setApplicationLocales) funcione también en Android 8–12.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()

    @Inject
    lateinit var activeWorkoutManager: ActiveWorkoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOpenWorkoutIntent(intent)
        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val miniBarState by viewModel.miniBarState.collectAsStateWithLifecycle()
            val openActiveWorkoutSignal by viewModel.openActiveWorkoutSignal.collectAsStateWithLifecycle()
            FerroTheme(themeMode = themeMode) {
                FerroNavHost(
                    miniBarState = miniBarState,
                    openActiveWorkoutSignal = openActiveWorkoutSignal
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenWorkoutIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch {
            activeWorkoutManager.restoreIfNeeded()
            activeWorkoutManager.syncNotificationForForeground()
        }
    }

    override fun onStop() {
        lifecycleScope.launch {
            activeWorkoutManager.flushNow()
        }
        super.onStop()
    }

    private fun handleOpenWorkoutIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ACTIVE_WORKOUT, false) == true) {
            intent.removeExtra(EXTRA_OPEN_ACTIVE_WORKOUT)
            viewModel.requestOpenActiveWorkout()
        }
    }

    companion object {
        const val ACTION_OPEN_ACTIVE_WORKOUT = "com.davidguijo.ferro.OPEN_ACTIVE_WORKOUT"
        const val EXTRA_OPEN_ACTIVE_WORKOUT = "extra_open_active_workout"
    }
}
