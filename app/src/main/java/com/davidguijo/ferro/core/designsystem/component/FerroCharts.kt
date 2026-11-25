package com.davidguijo.ferro.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.davidguijo.ferro.core.designsystem.theme.FerroTheme
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.ColumnCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlin.math.roundToInt

/** Punto genérico indexado para gráficas cartesianas de Ferro. */
data class FerroChartEntry(val xLabel: String, val tooltipTitle: String, val value: Double, val formattedValue: String)

private val FerroChartHeight = 220.dp
private val FerroChartColumnWidth = 14.dp
private val FerroChartPointSize = 8.dp
private val FerroChartStrokeWidth = 2.dp
private val FerroChartAxisLineWidth = 1.dp

/**
 * Gráfica de línea temporal basada en Vico 3 con colores y tipografía de [FerroTheme].
 * Al tocar un punto, muestra un marcador interactivo con fecha y valor formateado.
 */
@Composable
fun FerroLineChart(
    entries: List<FerroChartEntry>,
    yAxisValueFormatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val colors = FerroTheme.colors
    val typography = FerroTheme.typography
    val spacing = FerroTheme.spacing
    val radius = FerroTheme.radius

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            lineModel {
                series(
                    x = entries.indices.map { it.toDouble() },
                    y = entries.map { it.value }
                )
            }
        }
    }

    val vicoTheme =
        rememberM3VicoTheme(
            columnCartesianLayerColors = listOf(colors.ink),
            lineCartesianLayerColors = listOf(colors.ink),
            lineColor = colors.line,
            textColor = colors.inkMuted
        )

    val axisLabel =
        rememberTextComponent(
            style = typography.label.copy(color = colors.inkMuted),
            margins = Insets(horizontal = spacing.space1, vertical = spacing.space1)
        )
    val axisLine = rememberLineComponent(fill = Fill(colors.line), thickness = FerroChartAxisLineWidth)
    val guideline = rememberLineComponent(fill = Fill(colors.line), thickness = FerroChartAxisLineWidth)

    val pointComponent =
        rememberShapeComponent(
            fill = Fill(colors.ink),
            shape = CircleShape,
            strokeFill = Fill(colors.bg),
            strokeThickness = FerroChartStrokeWidth
        )
    val line =
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(colors.ink)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = FerroChartStrokeWidth),
            pointProvider =
                LineCartesianLayer.PointProvider.single(
                    LineCartesianLayer.Point(
                        component = pointComponent,
                        size = FerroChartPointSize
                    )
                )
        )

    val startAxisFormatter =
        remember(yAxisValueFormatter) {
            CartesianValueFormatter { _, value, _ -> yAxisValueFormatter(value) }
        }
    val bottomAxisFormatter =
        remember(entries) {
            CartesianValueFormatter { _, value, _ ->
                val idx = value.roundToInt()
                entries.getOrNull(idx)?.xLabel ?: " "
            }
        }

    val markerLabel =
        rememberTextComponent(
            style = typography.caption.copy(color = colors.ink),
            lineCount = 2,
            padding = Insets(horizontal = spacing.space2, vertical = spacing.space1),
            background =
                rememberShapeComponent(
                    fill = Fill(colors.surfaceRaised),
                    shape = RoundedCornerShape(radius.sm),
                    strokeFill = Fill(colors.lineStrong),
                    strokeThickness = FerroChartAxisLineWidth
                )
        )
    val markerGuideline =
        rememberLineComponent(
            fill = Fill(colors.lineStrong),
            thickness = FerroChartAxisLineWidth
        )
    val markerFormatter =
        remember(entries) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val lineTarget = targets.filterIsInstance<LineCartesianLayerMarkerTarget>().firstOrNull()
                val idx = lineTarget?.points?.firstOrNull()?.entry?.x?.roundToInt()
                val item = idx?.let { entries.getOrNull(it) }
                if (item != null) {
                    "${item.tooltipTitle}\n${item.formattedValue}"
                } else {
                    ""
                }
            }
        }
    val marker =
        rememberDefaultCartesianMarker(
            label = markerLabel,
            valueFormatter = markerFormatter,
            guideline = markerGuideline
        )

    ProvideVicoTheme(vicoTheme) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberLineCartesianLayer(
                        lineProvider = LineCartesianLayer.LineProvider.series(line)
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            line = axisLine,
                            label = axisLabel,
                            tick = null,
                            guideline = guideline,
                            valueFormatter = startAxisFormatter
                        ),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            line = axisLine,
                            label = axisLabel,
                            tick = null,
                            guideline = null,
                            valueFormatter = bottomAxisFormatter
                        ),
                    marker = marker
                ),
            modelProducer = modelProducer,
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(FerroChartHeight)
        )
    }
}

/**
 * Gráfica de barras basada en Vico 3 con colores y tipografía de [FerroTheme].
 * Al tocar una barra, muestra un marcador interactivo con periodo y valor formateado.
 */
@Composable
fun FerroColumnChart(
    entries: List<FerroChartEntry>,
    yAxisValueFormatter: (Double) -> String,
    modifier: Modifier = Modifier
) {
    if (entries.isEmpty()) return

    val colors = FerroTheme.colors
    val typography = FerroTheme.typography
    val spacing = FerroTheme.spacing
    val radius = FerroTheme.radius

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            columnModel {
                series(
                    x = entries.indices.map { it.toDouble() },
                    y = entries.map { it.value }
                )
            }
        }
    }

    val vicoTheme =
        rememberM3VicoTheme(
            columnCartesianLayerColors = listOf(colors.ink),
            lineCartesianLayerColors = listOf(colors.ink),
            lineColor = colors.line,
            textColor = colors.inkMuted
        )

    val axisLabel =
        rememberTextComponent(
            style = typography.label.copy(color = colors.inkMuted),
            margins = Insets(horizontal = spacing.space1, vertical = spacing.space1)
        )
    val axisLine = rememberLineComponent(fill = Fill(colors.line), thickness = FerroChartAxisLineWidth)
    val guideline = rememberLineComponent(fill = Fill(colors.line), thickness = FerroChartAxisLineWidth)

    val columnComponent =
        rememberLineComponent(
            fill = Fill(colors.ink),
            thickness = FerroChartColumnWidth,
            shape = RoundedCornerShape(topStart = radius.sm, topEnd = radius.sm)
        )

    val startAxisFormatter =
        remember(yAxisValueFormatter) {
            CartesianValueFormatter { _, value, _ -> yAxisValueFormatter(value) }
        }
    val bottomAxisFormatter =
        remember(entries) {
            CartesianValueFormatter { _, value, _ ->
                val idx = value.roundToInt()
                entries.getOrNull(idx)?.xLabel ?: " "
            }
        }

    val markerLabel =
        rememberTextComponent(
            style = typography.caption.copy(color = colors.ink),
            lineCount = 2,
            padding = Insets(horizontal = spacing.space2, vertical = spacing.space1),
            background =
                rememberShapeComponent(
                    fill = Fill(colors.surfaceRaised),
                    shape = RoundedCornerShape(radius.sm),
                    strokeFill = Fill(colors.lineStrong),
                    strokeThickness = FerroChartAxisLineWidth
                )
        )
    val markerGuideline =
        rememberLineComponent(
            fill = Fill(colors.lineStrong),
            thickness = FerroChartAxisLineWidth
        )
    val markerFormatter =
        remember(entries) {
            DefaultCartesianMarker.ValueFormatter { _, targets ->
                val colTarget = targets.filterIsInstance<ColumnCartesianLayerMarkerTarget>().firstOrNull()
                val idx = colTarget?.columns?.firstOrNull()?.entry?.x?.roundToInt()
                val item = idx?.let { entries.getOrNull(it) }
                if (item != null) {
                    "${item.tooltipTitle}\n${item.formattedValue}"
                } else {
                    ""
                }
            }
        }
    val marker =
        rememberDefaultCartesianMarker(
            label = markerLabel,
            valueFormatter = markerFormatter,
            guideline = markerGuideline
        )

    ProvideVicoTheme(vicoTheme) {
        CartesianChartHost(
            chart =
                rememberCartesianChart(
                    rememberColumnCartesianLayer(
                        columnProvider = ColumnCartesianLayer.ColumnProvider.series(columnComponent)
                    ),
                    startAxis =
                        VerticalAxis.rememberStart(
                            line = axisLine,
                            label = axisLabel,
                            tick = null,
                            guideline = guideline,
                            valueFormatter = startAxisFormatter
                        ),
                    bottomAxis =
                        HorizontalAxis.rememberBottom(
                            line = axisLine,
                            label = axisLabel,
                            tick = null,
                            guideline = null,
                            valueFormatter = bottomAxisFormatter
                        ),
                    marker = marker
                ),
            modelProducer = modelProducer,
            modifier =
                modifier
                    .fillMaxWidth()
                    .height(FerroChartHeight)
        )
    }
}
