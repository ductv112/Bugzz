package com.bugzz.filter.camera.ui.camera

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch

/**
 * Horizontal filter picker strip (04-CONTEXT D-15/D-16/D-17/D-18 + 04-UI-SPEC §Component Specs §2).
 *
 * Layout: Surface 100dp tall, 80%-alpha dark overlay; LazyRow of 88dp slots (72dp thumbnail +
 * 8dp vertical padding each side) with 8dp item spacing + 8dp content padding.
 *
 * Selection: 2dp white border + 1.15× scale + 200ms FastOutSlowInEasing transition. Tap invokes
 * [onSelect] + animates center-snap via [rememberLazyListState].animateScrollToItem.
 *
 * Accessibility: each item has [Role.Button] + contentDescription "$displayName filter[, selected]".
 *
 * Empty-list guard: if [filters] is empty, composable emits a zero-height Spacer and returns
 * immediately — prevents animateScrollToItem IOOBE (04-RESEARCH Pitfall 6).
 *
 * @param filters     Ordered list of filters to display (from [CameraUiState.filters]).
 * @param selectedId  ID of the currently-selected filter (from [CameraUiState.selectedFilterId]).
 * @param onSelect    Called with the filter ID when user taps a thumbnail.
 * @param modifier    Optional [Modifier] for the strip container (used to position in parent Box).
 */
@Composable
fun FilterPicker(
    filters: List<FilterSummary>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (filters.isEmpty()) {
        // Defensive per 04-UI-SPEC §Copywriting "Empty state" + Pitfall 6 — strip collapses to 0dp.
        Spacer(modifier = modifier.height(0.dp))
        return
    }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val hapticView = LocalView.current

    // Center-snap on first paint + any external selectedId change (e.g. DataStore restore).
    // Use instant scrollToItem on LaunchedEffect to avoid a visible animated scroll on entry;
    // subsequent taps use animateScrollToItem for the animated snap (04-UI-SPEC §Motion Specs).
    LaunchedEffect(selectedId, filters.size) {
        val idx = filters.indexOfFirst { it.id == selectedId }
        if (idx >= 0) {
            listState.scrollToItem(idx)
        }
    }

    Surface(
        color = Color(0xCC1E1E1E),
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filters, key = { it.id }) { filter ->
                val isSelected = filter.id == selectedId
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                    label = "filterScale_${filter.id}",
                )
                val contentDesc = if (isSelected) "${filter.displayName} filter, selected"
                                  else "${filter.displayName} filter"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .size(width = 88.dp, height = 92.dp)
                        .padding(vertical = 4.dp)
                        .clickable {
                            hapticView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            onSelect(filter.id)
                            val idx = filters.indexOfFirst { it.id == filter.id }
                            if (idx >= 0) {
                                scope.launch { listState.animateScrollToItem(idx, scrollOffset = 0) }
                            }
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = contentDesc
                        },
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("file:///android_asset/${filter.assetDir}/frame_00.png")
                            .crossfade(true)
                            .build(),
                        contentDescription = null, // parent Column carries semantics
                        contentScale = ContentScale.Crop,
                        placeholder = ColorPainter(Color(0xFF2A2A2A)),
                        error = ColorPainter(Color(0xFF2A2A2A)),
                        modifier = Modifier
                            .size(72.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                border = BorderStroke(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                ),
                                shape = RoundedCornerShape(12.dp),
                            ),
                    )
                    Text(
                        text = filter.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .width(72.dp)
                            .padding(top = 2.dp),
                    )
                }
            }
        }
    }
}
