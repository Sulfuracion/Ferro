package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme

/**
 * Elemento de lista Ferro con alto mínimo `rowHeight` (52 dp),
 * título en `bodyStrong`, subtítulo opcional en `caption` y slots laterales.
 */
@Composable
fun FerroListItem(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(onClick = onClick)
        } else {
            Modifier
        }

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = FerroTheme.sizes.rowHeight)
                .then(clickModifier)
                .padding(
                    horizontal = FerroTheme.spacing.space4,
                    vertical = FerroTheme.spacing.space3
                ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingContent != null) {
            leadingContent()
            Spacer(modifier = Modifier.width(FerroTheme.spacing.space3))
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = FerroTheme.typography.bodyStrong,
                color = FerroTheme.colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = FerroTheme.typography.caption,
                    color = FerroTheme.colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(FerroTheme.spacing.space3))
            trailingContent()
        }
    }
}

@FerroPreviews
@Composable
private fun FerroListItemPreview() {
    FerroTheme {
        Column(modifier = Modifier.background(FerroTheme.colors.surface)) {
            FerroListItem(
                title = "Press banca con barra",
                subtitle = "Pecho · Barra",
                leadingContent = {
                    Icon(
                        painter = painterResource(R.drawable.ic_fitness_center),
                        contentDescription = null,
                        modifier = Modifier.size(FerroTheme.sizes.iconMd),
                        tint = FerroTheme.colors.inkMuted
                    )
                },
                trailingContent = {
                    Text(
                        text = "100 kg",
                        style = FerroTheme.typography.number,
                        color = FerroTheme.colors.ink
                    )
                },
                onClick = {}
            )
            FerroDivider()
            FerroListItem(
                title = "Sentadilla trasera",
                subtitle = "Cuádriceps · Barra",
                onClick = {}
            )
        }
    }
}
