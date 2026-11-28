package com.emergencysupply.rations.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergencysupply.rations.ui.NavTab
import com.emergencysupply.rations.ui.theme.BorderSlate
import com.emergencysupply.rations.ui.theme.CardSurface
import com.emergencysupply.rations.ui.theme.CobaltBlue
import com.emergencysupply.rations.ui.theme.CobaltSoft
import com.emergencysupply.rations.ui.theme.DangerRed
import com.emergencysupply.rations.ui.theme.DangerSoft
import com.emergencysupply.rations.ui.theme.EmeraldVibrant
import com.emergencysupply.rations.ui.theme.IndigoAccent
import com.emergencysupply.rations.ui.theme.IndigoSoft
import com.emergencysupply.rations.ui.theme.InfoBlue
import com.emergencysupply.rations.ui.theme.InfoSoft
import com.emergencysupply.rations.ui.theme.InkBlack
import com.emergencysupply.rations.ui.theme.LightSlate
import com.emergencysupply.rations.ui.theme.MediumSlate
import com.emergencysupply.rations.ui.theme.MidnightSlate
import com.emergencysupply.rations.ui.theme.MutedSlate
import com.emergencysupply.rations.ui.theme.OrangeAccent
import com.emergencysupply.rations.ui.theme.OrangeSoft
import com.emergencysupply.rations.ui.theme.PinkAccent
import com.emergencysupply.rations.ui.theme.PinkSoft
import com.emergencysupply.rations.ui.theme.PurpleAccent
import com.emergencysupply.rations.ui.theme.PurpleSoft
import com.emergencysupply.rations.ui.theme.SoftTileSurface
import com.emergencysupply.rations.ui.theme.SuccessGreen
import com.emergencysupply.rations.ui.theme.SuccessSoft
import com.emergencysupply.rations.ui.theme.TealAccent
import com.emergencysupply.rations.ui.theme.TealPrimary
import com.emergencysupply.rations.ui.theme.TealSoft
import com.emergencysupply.rations.ui.theme.WarningAmber
import com.emergencysupply.rations.ui.theme.WarningSoft
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Reusable dashed border modifier with rounded corners (Budgetly / Wonder Bites styled).
 */
fun Modifier.dashedBorder(
    width: Dp = 1.5.dp,
    color: Color,
    cornerRadius: Dp = 16.dp
): Modifier = this.drawBehind {
    val strokeWidthPx = width.toPx()
    val halfStroke = strokeWidthPx / 2f
    val cornerRadiusPx = cornerRadius.toPx()
    val pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

    drawRoundRect(
        color = color,
        topLeft = Offset(halfStroke, halfStroke),
        size = Size(
            width = size.width - strokeWidthPx,
            height = size.height - strokeWidthPx
        ),
        cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        style = Stroke(
            width = strokeWidthPx,
            pathEffect = pathEffect
        )
    )
}

data class CategoryVisual(val tint: Color, val softBg: Color)

fun categoryVisualFor(category: String): CategoryVisual {
    val lower = category.lowercase()
    return when {
        lower.contains("grain") || lower.contains("rice") || lower.contains("pasta") || lower.contains("flour") ->
            CategoryVisual(WarningAmber, WarningAmber)
        lower.contains("halal") || lower.contains("meat") || lower.contains("freezer") ->
            CategoryVisual(DangerRed, DangerRed)
        lower.contains("protein") || lower.contains("lentil") ->
            CategoryVisual(OrangeAccent, OrangeAccent)
        lower.contains("can") || lower.contains("produce") ->
            CategoryVisual(SuccessGreen, SuccessGreen)
        lower.contains("baby") || lower.contains("kid") ->
            CategoryVisual(PurpleAccent, PurpleAccent)
        lower.contains("dairy") || lower.contains("milk") || lower.contains("breakfast") ->
            CategoryVisual(IndigoAccent, IndigoAccent)
        lower.contains("water") || lower.contains("drink") ->
            CategoryVisual(CobaltBlue, CobaltBlue)
        lower.contains("oil") || lower.contains("spice") || lower.contains("ghee") ->
            CategoryVisual(TealPrimary, TealPrimary)
        lower.contains("snack") || lower.contains("bakery") || lower.contains("bar") ->
            CategoryVisual(OrangeAccent, OrangeAccent)
        lower.contains("essential") || lower.contains("aid") || lower.contains("med") ->
            CategoryVisual(PinkAccent, PinkAccent)
        else ->
            CategoryVisual(CobaltBlue, CobaltBlue)
    }
}

fun locationVisualFor(location: String): CategoryVisual {
    val lower = location.trim().lowercase()
    return when {
        lower.contains("pantry") -> CategoryVisual(EmeraldVibrant, TealSoft)
        lower.contains("basement") || lower.contains("shelf") -> CategoryVisual(CobaltBlue, CobaltSoft)
        lower.contains("freezer") || lower.contains("deep") -> CategoryVisual(InfoBlue, InfoSoft)
        lower.contains("garage") || lower.contains("bin") -> CategoryVisual(OrangeAccent, OrangeSoft)
        lower.contains("kitchen") || lower.contains("cabinet") -> CategoryVisual(PurpleAccent, PurpleSoft)
        lower.contains("closet") || lower.contains("hallway") -> CategoryVisual(PinkAccent, PinkSoft)
        else -> {
            val fallbackColors = listOf(
                EmeraldVibrant to TealSoft,
                CobaltBlue to CobaltSoft,
                InfoBlue to InfoSoft,
                OrangeAccent to OrangeSoft,
                PurpleAccent to PurpleSoft,
                PinkAccent to PinkSoft,
                IndigoAccent to IndigoSoft,
                WarningAmber to WarningSoft
            )
            val hash = kotlin.math.abs(location.hashCode())
            val (tint, soft) = fallbackColors[hash % fallbackColors.size]
            CategoryVisual(tint, soft)
        }
    }
}

data class WheelColumnSpec(
    val label: String,
    val items: List<String>,
    val selected: String,
    val weight: Float = 1f,
    val displayTransform: (String) -> String = { it },
    val onSelected: (String) -> Unit,
    val onAddClick: (() -> Unit)? = null
)

@Composable
fun ReadinessArcGauge(
    scorePct: Int,
    daysOfSupply: Int,
    modifier: Modifier = Modifier
) {
    val clampedPct = scorePct.coerceIn(0, 100)
    val healthLabel = when {
        clampedPct >= 80 -> "Strong"
        clampedPct >= 50 -> "Building"
        else -> "Restocking"
    }
    val badgeBg = when {
        clampedPct >= 80 -> SuccessSoft
        clampedPct >= 50 -> WarningSoft
        else -> DangerSoft
    }
    val badgeFg = when {
        clampedPct >= 80 -> SuccessGreen
        clampedPct >= 50 -> WarningAmber
        else -> DangerRed
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(210.dp, 165.dp)) {
            val strokePx = 18.dp.toPx()
            val arcSize = Size(size.width - strokePx, size.width - strokePx)
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            // Soft track arc (220 degrees sweep from 160)
            drawArc(
                color = Color(0xFFE8ECF4),
                startAngle = 160f,
                sweepAngle = 220f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            val activeSweep = (220f * (clampedPct / 100f)).coerceAtLeast(if (clampedPct > 0) 8f else 0f)
            if (activeSweep > 0f) {
                drawArc(
                    brush = Brush.horizontalGradient(listOf(TealPrimary, IndigoAccent)),
                    startAngle = 160f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 18.dp)
        ) {
            Surface(shape = RoundedCornerShape(10.dp), color = badgeBg) {
                Text(
                    text = healthLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeFg,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$daysOfSupply",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = MidnightSlate,
                    lineHeight = 36.sp
                )
                Text(
                    text = "d",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MediumSlate,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                )
            }
            Text(
                text = "$clampedPct% of Par Target",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = LightSlate
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableWheelDatePicker(
    initialDate: String,
    onDateSelected: (String) -> Unit
) {
    val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val currentYear = LocalDate.now().year
    val years = remember { (currentYear..currentYear + 10).toList() }

    val parsedInitial = remember(initialDate) {
        try {
            if (initialDate.isNotBlank()) LocalDate.parse(initialDate.trim()) else null
        } catch (_: Exception) {
            null
        }
    }

    var noExpiry by remember(initialDate) { mutableStateOf(initialDate.isBlank()) }
    var selectedMonth by remember { mutableStateOf((parsedInitial?.monthValue ?: LocalDate.now().plusYears(1).monthValue) - 1) } // 0..11
    var selectedDay by remember { mutableStateOf((parsedInitial?.dayOfMonth ?: LocalDate.now().dayOfMonth) - 1) } // 0..30
    var selectedYearIdx by remember {
        mutableStateOf(
            years.indexOf(parsedInitial?.year ?: (currentYear + 1)).let { if (it >= 0) it else 1 }
        )
    }

    val daysInMonth = remember(selectedMonth, selectedYearIdx) {
        YearMonth.of(years[selectedYearIdx], selectedMonth + 1).lengthOfMonth()
    }
    val clampedDay = selectedDay.coerceIn(0, daysInMonth - 1)
    val daysList = remember(daysInMonth) { (1..daysInMonth).map { "%02d".format(it) } }

    val monthState = rememberLazyListState(initialFirstVisibleItemIndex = selectedMonth)
    val dayState = rememberLazyListState(initialFirstVisibleItemIndex = clampedDay)
    val yearState = rememberLazyListState(initialFirstVisibleItemIndex = selectedYearIdx)
    val scope = rememberCoroutineScope()

    fun emitDate(mIdx: Int, dIdx: Int, yIdx: Int) {
        val y = years[yIdx.coerceIn(0, years.lastIndex)]
        val m = mIdx.coerceIn(0, 11) + 1
        val maxD = YearMonth.of(y, m).lengthOfMonth()
        val d = (dIdx + 1).coerceIn(1, maxD)
        noExpiry = false
        onDateSelected(LocalDate.of(y, m, d).format(DateTimeFormatter.ISO_LOCAL_DATE))
    }

    LaunchedEffect(monthState) {
        snapshotFlow { monthState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val idx = monthState.firstVisibleItemIndex.coerceIn(0, 11)
                if (idx != selectedMonth) {
                    selectedMonth = idx
                    emitDate(selectedMonth, clampedDay, selectedYearIdx)
                }
            }
    }

    LaunchedEffect(dayState) {
        snapshotFlow { dayState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val idx = dayState.firstVisibleItemIndex.coerceIn(0, daysList.lastIndex)
                if (idx != selectedDay) {
                    selectedDay = idx
                    emitDate(selectedMonth, selectedDay, selectedYearIdx)
                }
            }
    }

    LaunchedEffect(yearState) {
        snapshotFlow { yearState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val idx = yearState.firstVisibleItemIndex.coerceIn(0, years.lastIndex)
                if (idx != selectedYearIdx) {
                    selectedYearIdx = idx
                    emitDate(selectedMonth, clampedDay, selectedYearIdx)
                }
            }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Quick scrollable preset pills
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val presets = listOf("+3 Mo" to 3L, "+6 Mo" to 6L, "+1 Yr" to 12L, "+2 Yrs" to 24L, "+3 Yrs" to 36L)
            items(presets) { (label, mos) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftTileSurface,
                    border = BorderStroke(1.dp, BorderSlate),
                    modifier = Modifier.clickable {
                        val target = LocalDate.now().plusMonths(mos)
                        val yIdx = years.indexOf(target.year).coerceAtLeast(0)
                        val mIdx = target.monthValue - 1
                        val dIdx = target.dayOfMonth - 1
                        selectedYearIdx = yIdx
                        selectedMonth = mIdx
                        selectedDay = dIdx
                        noExpiry = false
                        onDateSelected(target.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        scope.launch {
                            monthState.animateScrollToItem(mIdx)
                            dayState.animateScrollToItem(dIdx)
                            yearState.animateScrollToItem(yIdx)
                        }
                    }
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MidnightSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (noExpiry) InkBlack else SoftTileSurface,
                    border = BorderStroke(1.dp, if (noExpiry) InkBlack else BorderSlate),
                    modifier = Modifier.clickable {
                        noExpiry = true
                        onDateSelected("")
                    }
                ) {
                    Text(
                        text = "No Expiry",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (noExpiry) Color.White else MediumSlate,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (noExpiry) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { emitDate(selectedMonth, clampedDay, selectedYearIdx) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No Expiration Date • Tap to set scrollable date",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MediumSlate
                    )
                }
            }
        } else {
            // Scrollable 3-column Drum Wheel Picker
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftTileSurface,
                border = BorderStroke(1.dp, BorderSlate),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // Center active band
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, EmeraldVibrant.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                            .height(40.dp)
                    ) {}

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WheelColumn(
                            items = months,
                            state = monthState,
                            selectedIdx = selectedMonth,
                            onItemClick = { idx ->
                                selectedMonth = idx
                                emitDate(selectedMonth, clampedDay, selectedYearIdx)
                                scope.launch { monthState.animateScrollToItem(idx) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        WheelColumn(
                            items = daysList,
                            state = dayState,
                            selectedIdx = clampedDay,
                            onItemClick = { idx ->
                                selectedDay = idx
                                emitDate(selectedMonth, selectedDay, selectedYearIdx)
                                scope.launch { dayState.animateScrollToItem(idx) }
                            },
                            modifier = Modifier.weight(0.8f)
                        )
                        WheelColumn(
                            items = years.map { it.toString() },
                            state = yearState,
                            selectedIdx = selectedYearIdx,
                            onItemClick = { idx ->
                                selectedYearIdx = idx
                                emitDate(selectedMonth, clampedDay, selectedYearIdx)
                                scope.launch { yearState.animateScrollToItem(idx) }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScrollableMultiWheelPicker(
    columns: List<WheelColumnSpec>,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    accentColor: Color = EmeraldVibrant
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Compact column header labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { col ->
                Row(
                    modifier = Modifier.weight(col.weight),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = col.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MediumSlate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (col.onAddClick != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { col.onAddClick.invoke() }
                        ) {
                            Text(
                                text = "+ New",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical drum-wheel box
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SoftTileSurface,
            border = BorderStroke(1.dp, BorderSlate),
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Centered white selection band
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    shadowElevation = 1.dp,
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(40.dp)
                ) {}

                // Drum columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    columns.forEach { col ->
                        MultiWheelColumn(
                            col = col,
                            height = height,
                            accentColor = accentColor,
                            modifier = Modifier.weight(col.weight)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MultiWheelColumn(
    col: WheelColumnSpec,
    height: Dp,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val items by rememberUpdatedState(col.items)
    val onSelected by rememberUpdatedState(col.onSelected)
    val selectedIdx = remember(items, col.selected) {
        val idx = items.indexOf(col.selected)
        if (idx >= 0) idx else 0
    }

    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIdx)
    val scope = rememberCoroutineScope()

    LaunchedEffect(col.items, selectedIdx) {
        if (items.isNotEmpty() && selectedIdx in items.indices) {
            if (state.firstVisibleItemIndex != selectedIdx) {
                state.animateScrollToItem(selectedIdx)
            }
        }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .filter { !it }
            .collect {
                val currentItems = items
                if (currentItems.isNotEmpty()) {
                    val idx = state.firstVisibleItemIndex.coerceIn(0, currentItems.lastIndex)
                    val candidate = currentItems[idx]
                    if (candidate != col.selected) {
                        onSelected(candidate)
                    }
                }
            }
    }

    WheelColumn(
        items = items,
        state = state,
        selectedIdx = selectedIdx,
        onItemClick = { idx ->
            val currentItems = items
            if (idx in currentItems.indices) {
                onSelected(currentItems[idx])
                scope.launch { state.animateScrollToItem(idx) }
            }
        },
        displayTransform = col.displayTransform,
        highlightColor = accentColor,
        height = height,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    state: androidx.compose.foundation.lazy.LazyListState,
    selectedIdx: Int,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 132.dp,
    highlightColor: Color = EmeraldVibrant,
    displayTransform: (String) -> String = { it }
) {
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = state)
    val verticalPadding = (height - 40.dp).coerceAtLeast(0.dp) / 2
    LazyColumn(
        state = state,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = verticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(height)
    ) {
        items(items.size) { index ->
            val isCentered = index == selectedIdx
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clickable { onItemClick(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayTransform(items[index]),
                    fontSize = if (isCentered) 15.sp else 13.sp,
                    fontWeight = if (isCentered) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (isCentered) highlightColor else MutedSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FloatingCapsuleBottomBar(
    currentTab: NavTab,
    restockBadgeCount: Int,
    groceriesBadgeCount: Int,
    onSelectTab: (NavTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = CardSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, BorderSlate.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CapsuleNavItem(
                    icon = Icons.Default.Dashboard,
                    label = "Home",
                    selected = currentTab == NavTab.DASHBOARD,
                    badgeCount = restockBadgeCount,
                    badgeColor = DangerRed,
                    onClick = { onSelectTab(NavTab.DASHBOARD) }
                )
                CapsuleNavItem(
                    icon = Icons.Default.Inventory2,
                    label = "Pantry",
                    selected = currentTab == NavTab.INVENTORY,
                    badgeCount = 0,
                    badgeColor = TealPrimary,
                    onClick = { onSelectTab(NavTab.INVENTORY) }
                )
                CapsuleNavItem(
                    icon = Icons.Default.ShoppingCart,
                    label = "Shop",
                    selected = currentTab == NavTab.GROCERIES,
                    badgeCount = groceriesBadgeCount,
                    badgeColor = OrangeAccent,
                    onClick = { onSelectTab(NavTab.GROCERIES) }
                )
                CapsuleNavItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    selected = currentTab == NavTab.SETTINGS,
                    badgeCount = 0,
                    badgeColor = TealPrimary,
                    onClick = { onSelectTab(NavTab.SETTINGS) }
                )
            }
        }
    }
}

@Composable
private fun CapsuleNavItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    badgeCount: Int,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (selected) InkBlack else Color.Transparent,
        modifier = Modifier
            .height(46.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (selected) 14.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (badgeCount > 0) {
                BadgedBox(badge = {
                    Badge(containerColor = badgeColor) {
                        Text("$badgeCount", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (selected) Color.White else MediumSlate,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) Color.White else MediumSlate,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (selected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
