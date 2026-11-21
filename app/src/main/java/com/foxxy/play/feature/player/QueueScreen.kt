package com.foxxy.play.feature.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch

@Composable
fun QueueScreen(
    viewModel: FoxxyPlayerViewModel,
    onBack: () -> Unit
) {
    val queue by viewModel.queue.collectAsState()
    val currentIndex by viewModel.queueIndex.collectAsState()

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var displayedQueue by remember {
        mutableStateOf(queue)
    }

    val latestQueue by rememberUpdatedState(displayedQueue)

    var draggedKey by remember {
        mutableStateOf<Long?>(null)
    }

    var dragDistance by remember {
        mutableFloatStateOf(0f)
    }

    LaunchedEffect(queue) {
        if (draggedKey == null) {
            displayedQueue = queue
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Up Next",
                    style = MaterialTheme.typography.headlineMedium
                )

                Text(
                    "${queue.size} songs in queue",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (displayedQueue.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        "Your queue is empty.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(7.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                itemsIndexed(
                    displayedQueue,
                    key = { _, track -> track.id }
                ) { index, track ->

                    val isCurrent = index == currentIndex
                    val isDragged = draggedKey == track.id

                    val scale by animateFloatAsState(
                        targetValue = if (isDragged) 1.025f else 1f,
                        label = "queue-item-scale"
                    )

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .shadow(
                                elevation = if (isDragged) 10.dp else 0.dp,
                                shape = RoundedCornerShape(15.dp)
                            ),
                        shape = RoundedCornerShape(15.dp),
                        color = if (isCurrent)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.playQueueItem(index)
                                }
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = 9.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .pointerInput(track.id) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggedKey = track.id
                                                dragDistance = 0f
                                            },
                                            onDragEnd = {
                                                draggedKey = null
                                                dragDistance = 0f
                                            },
                                            onDragCancel = {
                                                draggedKey = null
                                                dragDistance = 0f
                                            },
                                            onDrag = { change, amount ->
                                                change.consume()

                                                if (draggedKey != track.id) {
                                                    return@detectDragGesturesAfterLongPress
                                                }

                                                dragDistance += amount.y

                                                val current = latestQueue
                                                val fromIndex = current.indexOfFirst {
                                                    it.id == track.id
                                                }

                                                if (fromIndex < 0) {
                                                    return@detectDragGesturesAfterLongPress
                                                }

                                                val draggedInfo =
                                                    listState.layoutInfo.visibleItemsInfo
                                                        .firstOrNull {
                                                            it.key == track.id
                                                        }
                                                        ?: return@detectDragGesturesAfterLongPress

                                                val centerY =
                                                    draggedInfo.offset +
                                                    draggedInfo.size / 2 +
                                                    dragDistance.toInt()

                                                val layout = listState.layoutInfo

                                                val targetInfo =
                                                    layout.visibleItemsInfo.firstOrNull {
                                                        centerY >= it.offset &&
                                                        centerY < it.offset + it.size
                                                    }
                                                    ?: return@detectDragGesturesAfterLongPress

                                                val toIndex = current.indexOfFirst {
                                                    it.id.toString() == targetInfo.key.toString()
                                                }

                                                if (
                                                    toIndex >= 0 &&
                                                    toIndex != fromIndex
                                                ) {
                                                    displayedQueue =
                                                        current.toMutableList().apply {
                                                            add(
                                                                toIndex,
                                                                removeAt(fromIndex)
                                                            )
                                                        }

                                                    viewModel.moveQueueItem(
                                                        fromIndex,
                                                        toIndex
                                                    )

                                                    dragDistance = 0f
                                                }

                                                val edge = 85

                                                if (
                                                    centerY <
                                                    layout.viewportStartOffset + edge
                                                ) {
                                                    scope.launch {
                                                        listState.scrollBy(-22f)
                                                    }
                                                } else if (
                                                    centerY >
                                                    layout.viewportEndOffset - edge
                                                ) {
                                                    scope.launch {
                                                        listState.scrollBy(22f)
                                                    }
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = "Hold to reorder",
                                    tint = if (isDragged || isCurrent)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.width(8.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    track.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isCurrent)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    track.artist,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (isCurrent) {
                                    Text(
                                        "NOW PLAYING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    viewModel.removeQueueItem(index)
                                }
                            ) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Remove from queue",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
