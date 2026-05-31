package com.example.cityapp.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged

/** Вертикальні колонки значень у стилі будильника (спрощений підбір після прокрутки). */
@Composable
fun WheelIntPicker(
    range: IntRange,
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    widthFraction: Float = 0.28f
) {
    val items = remember(range) { range.toList() }
    val startIdx = remember(items, value) { items.indexOf(value).coerceAtLeast(0) }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = startIdx)

    LaunchedEffect(value, items) {
        val idx = items.indexOf(value).coerceAtLeast(0)
        state.scrollToItem(idx)
    }

    LaunchedEffect(state, items) {
        snapshotFlow { state.firstVisibleItemIndex to state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { (idx, scrolling) ->
                if (!scrolling && idx in items.indices) {
                    val picked = items[idx]
                    if (picked != value) onValueChange(picked)
                }
            }
    }

    Column(modifier = modifier.fillMaxWidth(widthFraction), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Box(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            HorizontalDivider(Modifier.align(Alignment.Center))
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                items(count = items.size) { i ->
                    val v = items[i]
                    Text(
                        text = String.format("%02d", v),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        style = if (v == value) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WheelIntPickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelIntPicker(0..23, hour, onHourChange, "Год")
        WheelIntPicker(0..59, minute, onMinuteChange, "Хв")
    }
}
