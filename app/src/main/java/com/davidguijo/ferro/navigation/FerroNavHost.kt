package com.davidguijo.ferro.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroDivider
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.davidguijo.ferro.feature.backup.BackupRoute
import com.davidguijo.ferro.feature.backup.BackupScreen
import com.davidguijo.ferro.feature.body.BodyMeasurementsRoute
import com.davidguijo.ferro.feature.body.BodyMeasurementsScreen
import com.davidguijo.ferro.feature.exercises.ExerciseDetailRoute
import com.davidguijo.ferro.feature.exercises.ExerciseDetailScreen
import com.davidguijo.ferro.feature.exercises.ExerciseLibraryResult
import com.davidguijo.ferro.feature.exercises.ExerciseLibraryRoute
import com.davidguijo.ferro.feature.exercises.ExerciseLibraryScreen
import com.davidguijo.ferro.feature.exercises.ExerciseSelectionMode
import com.davidguijo.ferro.feature.history.CalendarRoute
import com.davidguijo.ferro.feature.history.CalendarScreen
import com.davidguijo.ferro.feature.history.HistoryRoute
import com.davidguijo.ferro.feature.history.HistoryScreen
import com.davidguijo.ferro.feature.history.SessionDetailRoute
import com.davidguijo.ferro.feature.history.SessionDetailScreen
import com.davidguijo.ferro.feature.progress.ProgressRoute
import com.davidguijo.ferro.feature.progress.ProgressScreen
import com.davidguijo.ferro.feature.routines.RoutineEditorRoute
import com.davidguijo.ferro.feature.routines.RoutineEditorScreen
import com.davidguijo.ferro.feature.routines.RoutinesRoute
import com.davidguijo.ferro.feature.routines.RoutinesScreen
import com.davidguijo.ferro.feature.settings.ProfileRoute
import com.davidguijo.ferro.feature.settings.ProfileScreen
import com.davidguijo.ferro.feature.settings.SettingsRoute
import com.davidguijo.ferro.feature.settings.SettingsScreen
import com.davidguijo.ferro.feature.workout.ActiveWorkoutMiniBar
import com.davidguijo.ferro.feature.workout.ActiveWorkoutMiniBarUiState
import com.davidguijo.ferro.feature.workout.ActiveWorkoutRoute
import com.davidguijo.ferro.feature.workout.ActiveWorkoutScreen
import com.davidguijo.ferro.feature.workout.TrainRoute
import com.davidguijo.ferro.feature.workout.TrainScreen
import com.davidguijo.ferro.feature.workout.WorkoutSummaryRoute
import com.davidguijo.ferro.feature.workout.WorkoutSummaryScreen
import kotlin.reflect.KClass

private enum class TopLevelTab(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    TRAIN(TrainRoute, TrainRoute::class, R.string.nav_train, R.drawable.ic_fitness_center),
    ROUTINES(RoutinesRoute, RoutinesRoute::class, R.string.nav_routines, R.drawable.ic_calendar_month),
    PROGRESS(ProgressRoute, ProgressRoute::class, R.string.nav_progress, R.drawable.ic_bar_chart),
    PROFILE(ProfileRoute, ProfileRoute::class, R.string.nav_profile, R.drawable.ic_person)
}

/** Único NavHost de la app. Las rutas son objetos/clases @Serializable (navegación tipada). */
@Composable
fun FerroNavHost(
    modifier: Modifier = Modifier,
    miniBarState: ActiveWorkoutMiniBarUiState? = null,
    openActiveWorkoutSignal: Int = 0
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar =
        TopLevelTab.entries.any { tab ->
            currentDestination?.hasRoute(tab.routeClass) == true
        }
    val showActiveWorkoutMiniBar = showBottomBar && miniBarState != null

    LaunchedEffect(openActiveWorkoutSignal) {
        if (openActiveWorkoutSignal > 0 && currentDestination?.hasRoute(ActiveWorkoutRoute::class) != true) {
            navController.navigate(ActiveWorkoutRoute()) {
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = FerroTheme.colors.bg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column {
                ActiveWorkoutMiniBar(
                    visible = showActiveWorkoutMiniBar,
                    workoutTitle = miniBarState?.workoutTitle.orEmpty(),
                    elapsedText = miniBarState?.elapsedText.orEmpty(),
                    restRemainingText = miniBarState?.restRemainingText,
                    restProgress = miniBarState?.restProgress,
                    onClick = {
                        navController.navigate(ActiveWorkoutRoute()) {
                            launchSingleTop = true
                        }
                    }
                )
                if (showBottomBar) {
                    FerroBottomBar(
                        isSelected = { tab -> currentDestination?.hasRoute(tab.routeClass) == true },
                        onSelectTab = { tab ->
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TrainRoute,
            modifier =
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
        ) {
            composable<TrainRoute> {
                TrainScreen(
                    onOpenActiveWorkout = {
                        navController.navigate(ActiveWorkoutRoute()) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<RoutinesRoute> {
                RoutinesScreen(
                    onOpenExerciseLibrary = { navController.navigate(ExerciseLibraryRoute()) },
                    onCreateRoutine = { navController.navigate(RoutineEditorRoute()) },
                    onEditRoutine = { routineId ->
                        navController.navigate(RoutineEditorRoute(routineId = routineId))
                    },
                    onStartWorkout = { routineId ->
                        navController.navigate(ActiveWorkoutRoute(routineId = routineId)) {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable<ProgressRoute> {
                ProgressScreen()
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onOpenExerciseLibrary = { navController.navigate(ExerciseLibraryRoute()) },
                    onOpenHistory = { navController.navigate(HistoryRoute) },
                    onOpenCalendar = { navController.navigate(CalendarRoute) },
                    onOpenBodyMeasurements = { navController.navigate(BodyMeasurementsRoute) },
                    onOpenBackup = { navController.navigate(BackupRoute) },
                    onOpenSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<ActiveWorkoutRoute> {
                ActiveWorkoutScreen(
                    onMinimize = { navController.popBackStack() },
                    onOpenExerciseDetail = { exerciseId ->
                        navController.navigate(ExerciseDetailRoute(exerciseId))
                    },
                    onPickExercisesToAdd = {
                        navController.navigate(
                            ExerciseLibraryRoute(selectionMode = ExerciseSelectionMode.MULTIPLE)
                        )
                    },
                    onPickExerciseToReplace = {
                        navController.navigate(
                            ExerciseLibraryRoute(selectionMode = ExerciseSelectionMode.SINGLE)
                        )
                    },
                    onWorkoutFinished = { workoutId ->
                        navController.navigate(WorkoutSummaryRoute(workoutId = workoutId)) {
                            popUpTo<ActiveWorkoutRoute> { inclusive = true }
                        }
                    },
                    onWorkoutDiscarded = { navController.popBackStack() }
                )
            }
            composable<WorkoutSummaryRoute> {
                WorkoutSummaryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<RoutineEditorRoute> {
                RoutineEditorScreen(
                    onBack = { navController.popBackStack() },
                    onPickExercises = {
                        navController.navigate(
                            ExerciseLibraryRoute(selectionMode = ExerciseSelectionMode.MULTIPLE)
                        )
                    }
                )
            }
            composable<ExerciseLibraryRoute> {
                ExerciseLibraryScreen(
                    onBack = { navController.popBackStack() },
                    onOpenExercise = { exerciseId ->
                        navController.navigate(ExerciseDetailRoute(exerciseId))
                    },
                    onSelectExercises = { selectedIds ->
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set(
                                ExerciseLibraryResult.SELECTED_EXERCISE_IDS_KEY,
                                ArrayList(selectedIds)
                            )
                        navController.popBackStack()
                    }
                )
            }
            composable<ExerciseDetailRoute> {
                ExerciseDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<HistoryRoute> {
                HistoryScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<SessionDetailRoute> {
                SessionDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<CalendarRoute> {
                CalendarScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<BodyMeasurementsRoute> {
                BodyMeasurementsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<BackupRoute> {
                BackupScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable<SettingsRoute> {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun FerroBottomBar(isSelected: (TopLevelTab) -> Boolean, onSelectTab: (TopLevelTab) -> Unit) {
    val colors = FerroTheme.colors
    Column {
        FerroDivider()
        NavigationBar(
            containerColor = colors.surface,
            contentColor = colors.ink
        ) {
            TopLevelTab.entries.forEach { tab ->
                val selected = isSelected(tab)
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            painter = painterResource(tab.iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(FerroTheme.sizes.iconMd)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(tab.labelRes),
                            style = FerroTheme.typography.caption,
                            maxLines = 1
                        )
                    },
                    colors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.brasaText,
                            selectedTextColor = colors.brasaText,
                            indicatorColor = colors.brasaSoft,
                            unselectedIconColor = colors.inkMuted,
                            unselectedTextColor = colors.inkMuted
                        )
                )
            }
        }
    }
}
