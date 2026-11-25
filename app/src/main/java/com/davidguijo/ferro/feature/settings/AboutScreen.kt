package com.davidguijo.ferro.feature.settings

import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.StringRes
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.davidguijo.ferro.R
import com.davidguijo.ferro.core.designsystem.component.FerroCard
import com.davidguijo.ferro.core.designsystem.component.FerroIconButton
import com.davidguijo.ferro.core.designsystem.component.FerroPreviews
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import kotlinx.serialization.Serializable

/** Pantalla «Acerca de» con versión, aviso de datos locales y licencias de librerías. */
@Serializable
data object AboutRoute

@Immutable
internal data class LibraryLicenseItem(val id: String, @StringRes val nameRes: Int, @StringRes val licenseRes: Int)

private val FerroLibraries =
    listOf(
        LibraryLicenseItem("compose", R.string.about_lib_compose, R.string.about_license_apache2),
        LibraryLicenseItem("androidx", R.string.about_lib_androidx, R.string.about_license_apache2),
        LibraryLicenseItem("room", R.string.about_lib_room, R.string.about_license_apache2),
        LibraryLicenseItem("hilt", R.string.about_lib_hilt, R.string.about_license_apache2),
        LibraryLicenseItem("kotlinx", R.string.about_lib_kotlinx, R.string.about_license_apache2),
        LibraryLicenseItem("vico", R.string.about_lib_vico, R.string.about_license_apache2),
        LibraryLicenseItem("coil", R.string.about_lib_coil, R.string.about_license_apache2),
        LibraryLicenseItem("barlow", R.string.about_lib_barlow, R.string.about_license_ofl)
    )

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionLabel =
        remember(context) {
            runCatching {
                val info =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        context.packageManager.getPackageInfo(context.packageName, 0)
                    }
                val name = info.versionName ?: "0.1.0"
                val code = PackageInfoCompat.getLongVersionCode(info)
                "$name ($code)"
            }.getOrDefault("0.1.0")
        }

    AboutContent(
        versionLabel = versionLabel,
        onBack = onBack,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AboutContent(versionLabel: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FerroTheme.colors
    val spacing = FerroTheme.spacing

    Scaffold(
        modifier = modifier,
        containerColor = colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = colors.bg,
                        titleContentColor = colors.ink
                    )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            contentPadding =
                PaddingValues(
                    start = spacing.space4,
                    end = spacing.space4,
                    top = spacing.space2,
                    bottom = spacing.space6
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.space3)
        ) {
            item(key = "header") {
                FerroCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.space4)
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .background(
                                        color = colors.iron,
                                        shape = RoundedCornerShape(FerroTheme.radius.md)
                                    ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_splash_logo),
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = colors.brasa
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = FerroTheme.typography.title,
                                color = colors.ink
                            )
                            Text(
                                text = stringResource(R.string.about_version_format, versionLabel),
                                style = FerroTheme.typography.caption,
                                color = colors.inkMuted
                            )
                        }
                    }
                }
            }

            item(key = "local_data") {
                FerroCard {
                    Column {
                        Text(
                            text = stringResource(R.string.about_local_data_title),
                            style = FerroTheme.typography.heading,
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(spacing.space2))
                        Text(
                            text = stringResource(R.string.about_local_data_body),
                            style = FerroTheme.typography.body,
                            color = colors.inkMuted
                        )
                    }
                }
            }

            item(key = "licenses_heading") {
                Spacer(modifier = Modifier.height(spacing.space2))
                Text(
                    text = stringResource(R.string.about_licenses_section).uppercase(),
                    style = FerroTheme.typography.label,
                    color = colors.inkMuted,
                    modifier = Modifier.semantics { heading() }
                )
            }

            items(
                items = FerroLibraries,
                key = { it.id }
            ) { item ->
                FerroCard {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = stringResource(item.nameRes),
                            style = FerroTheme.typography.bodyStrong,
                            color = colors.ink
                        )
                        Spacer(modifier = Modifier.height(spacing.space1))
                        Text(
                            text = stringResource(item.licenseRes),
                            style = FerroTheme.typography.caption,
                            color = colors.inkMuted
                        )
                    }
                }
            }
        }
    }
}

@FerroPreviews
@Composable
private fun AboutScreenPreview() {
    FerroTheme {
        AboutContent(
            versionLabel = "0.1.0 (1)",
            onBack = {}
        )
    }
}
