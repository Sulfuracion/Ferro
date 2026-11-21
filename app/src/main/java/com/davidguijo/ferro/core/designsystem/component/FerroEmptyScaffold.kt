package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Esqueleto común para pantallas en estado vacío con barra superior,
 * botón opcional de volver y [EmptyState] centrado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FerroEmptyScaffold(
    title: String,
    emptyTitle: String,
    emptyDescription: String,
    emptyIcon: Painter,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    emptyActionLabel: String? = null,
    onEmptyActionClick: (() -> Unit)? = null
) {
    val colors = FerroTheme.colors
    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        FerroIconButton(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = stringResource(R.string.action_back),
                            onClick = onBack
                        )
                    }
                },
                actions = actions,
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        }
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                title = emptyTitle,
                description = emptyDescription,
                icon = emptyIcon,
                actionLabel = emptyActionLabel,
                onActionClick = onEmptyActionClick
            )
        }
    }
}
