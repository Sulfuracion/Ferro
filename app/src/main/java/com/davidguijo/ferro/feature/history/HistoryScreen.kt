package com.davidguijo.ferro.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.EmptyState
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroErrorState
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroLoadingState
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenSessionDetail: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val pagedItems = viewModel.pagedItems.collectAsLazyPagingItems()
    HistoryContent(
        pagedItems = pagedItems,
        onBack = onBack,
        onOpenSessionDetail = onOpenSessionDetail,
        onOpenCalendar = onOpenCalendar,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HistoryContent(
    pagedItems: LazyPagingItems<HistoryListItem>,
    onBack: () -> Unit,
    onOpenSessionDetail: (String) -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.history_title),
                        style = FerroTheme.typography.title,
                        color = colors.ink
                    )
                },
                navigationIcon = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_arrow_back),
                        contentDescription = stringResource(R.string.action_back),
                        onClick = onBack
                    )
                },
                actions = {
                    FerroIconButton(
                        painter = painterResource(R.drawable.ic_calendar_month),
                        contentDescription = stringResource(R.string.history_open_calendar_cd),
                        onClick = onOpenCalendar
                    )
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        }
    ) { innerPadding ->
        val refreshState = pagedItems.loadState.refresh
        when {
            refreshState is LoadState.Loading && pagedItems.itemCount == 0 -> {
                FerroLoadingState(modifier = Modifier.padding(innerPadding))
            }

            refreshState is LoadState.Error && pagedItems.itemCount == 0 -> {
                FerroErrorState(
                    message = stringResource(R.string.error_generic_load),
                    onRetry = { pagedItems.retry() },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            refreshState is LoadState.NotLoading && pagedItems.itemCount == 0 -> {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        title = stringResource(R.string.history_empty_title),
                        description = stringResource(R.string.history_empty_description),
                        icon = painterResource(R.drawable.ic_calendar_month)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    contentPadding =
                        PaddingValues(
                            horizontal = spacing.space4,
                            vertical = spacing.space3
                        ),
                    verticalArrangement = Arrangement.spacedBy(spacing.space3)
                ) {
                    items(
                        count = pagedItems.itemCount,
                        key = pagedItems.itemKey { it.stableKey }
                    ) { index ->
                        when (val item = pagedItems[index]) {
                            is HistoryListItem.WeekHeader -> {
                                Text(
                                    text = item.label.uppercase(),
                                    style = FerroTheme.typography.label,
                                    color = colors.inkMuted,
                                    modifier = Modifier.padding(top = spacing.space2, bottom = spacing.space1)
                                )
                            }

                            is HistoryListItem.Session -> {
                                HistorySessionCard(
                                    session = item,
                                    onClick = { onOpenSessionDetail(item.workoutId) }
                                )
                            }

                            null -> Unit
                        }
                    }

                    if (pagedItems.loadState.append is LoadState.Loading) {
                        item(key = "append_loading") {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(spacing.space4),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = colors.brasa,
                                    modifier = Modifier.size(FerroTheme.sizes.iconMd)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HistorySessionCard(session: HistoryListItem.Session, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    FerroCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.name,
                    style = FerroTheme.typography.heading,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacing.space1))
                Text(
                    text = session.dateTimeText,
                    style = FerroTheme.typography.caption,
                    color = colors.inkMuted
                )
            }

            if (session.recordCount > 0) {
                Row(
                    modifier =
                        Modifier
                            .background(
                                color = colors.brasaSoft,
                                shape = RoundedCornerShape(FerroTheme.radius.sm)
                            )
                            .padding(horizontal = spacing.space2, vertical = spacing.space1),
                    horizontalArrangement = Arrangement.spacedBy(spacing.space1),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_trophy),
                        contentDescription = null,
                        tint = colors.brasaText,
                        modifier = Modifier.size(FerroTheme.sizes.iconSm)
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.history_records_count,
                            session.recordCount,
                            session.recordCount
                        ),
                        style = FerroTheme.typography.caption,
                        color = colors.brasaText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(spacing.space3))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space4)
        ) {
            HistoryStatColumn(
                label = stringResource(R.string.history_stat_duration),
                value = session.durationText ?: stringResource(R.string.history_duration_unknown),
                modifier = Modifier.weight(1f)
            )
            HistoryStatColumn(
                label = stringResource(R.string.history_stat_volume),
                value = session.volumeText,
                modifier = Modifier.weight(1f)
            )
            HistoryStatColumn(
                label = stringResource(R.string.history_stat_sets),
                value = session.completedSetsCount.toString(),
                modifier = Modifier.weight(0.7f)
            )
        }

        if (session.mainExerciseNames.isNotEmpty()) {
            Spacer(modifier = Modifier.height(spacing.space3))
            val moreExercisesText =
                if (session.extraExercisesCount > 0) {
                    pluralStringResource(
                        R.plurals.history_more_exercises,
                        session.extraExercisesCount,
                        session.extraExercisesCount
                    )
                } else {
                    null
                }
            val summaryText =
                buildString {
                    append(session.mainExerciseNames.joinToString(" · "))
                    if (moreExercisesText != null) {
                        append(" ")
                        append(moreExercisesText)
                    }
                }
            Text(
                text = summaryText,
                style = FerroTheme.typography.caption,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryStatColumn(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = FerroTheme.typography.label,
            color = colors.inkSubtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = FerroTheme.typography.number,
            color = colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
