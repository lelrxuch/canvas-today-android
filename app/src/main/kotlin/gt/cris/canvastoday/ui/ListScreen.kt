package gt.cris.canvastoday.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gt.cris.canvastoday.R
import gt.cris.canvastoday.core.NoteDraft
import gt.cris.canvastoday.core.Planner
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.core.courseColorIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

private const val MINUTE_MS = 60_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    onOpen: (String) -> Unit,
    onCreateNote: (NoteDraft, (Int?) -> Unit) -> Unit,
    onDeleteNote: (PlannerItem, (Int?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<PlannerItem?>(null) }

    // Las etiquetas relativas ("En 20 min") se recalculan cada minuto sin pedir datos.
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE_MS)
            now = ZonedDateTime.now()
        }
    }
    LaunchedEffect(state.snapshot) { now = ZonedDateTime.now() }

    // No se llama "items" para no chocar con LazyListScope.items.
    val all = state.snapshot.items
    val groups = Planner.grouped(all, now)
    val courses = all.filter { !it.isNote && it.courseId != null && it.course != null }
        .associate { it.courseId!! to it.course!! }
        .toList()
        .sortedBy { it.second }

    fun report(error: Int?) {
        if (error != null) scope.launch { snackbar.showSnackbar(context.getString(error)) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_list)) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, stringResource(R.string.action_refresh))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.action_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showNoteDialog = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.note_new)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            ) {
                item(key = "summary") { Summary(all, groups) }

                state.snapshot.error?.let { kind ->
                    item(key = "error") {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        ) {
                            Text(
                                stringResource(Labels.error(kind)),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }

                if (all.isEmpty() && state.snapshot.fetchedAt != null && state.snapshot.error == null) {
                    item(key = "empty") { Empty(state.daysAhead) }
                }

                for ((group, list) in groups) {
                    item(key = "head-${group.name}") { SectionHead(group, list.size) }
                    items(list, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            now = now,
                            expanded = expandedId == item.id,
                            onToggle = { expandedId = if (expandedId == item.id) null else item.id },
                            onOpen = onOpen,
                            onDelete = { toDelete = item },
                        )
                    }
                }

                item(key = "footer") {
                    val at = state.snapshot.fetchedAt
                    Text(
                        if (at != null) {
                            stringResource(R.string.updated_at, Labels.timeOf(at))
                        } else {
                            stringResource(R.string.never_updated)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp),
                    )
                }
            }
        }
    }

    if (showNoteDialog) {
        NoteDialog(
            courses = courses,
            onDismiss = { showNoteDialog = false },
            onSave = { draft, done ->
                onCreateNote(draft) { error ->
                    done(error)
                    if (error == null) showNoteDialog = false
                }
            },
        )
    }

    toDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            text = { Text(stringResource(R.string.note_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        toDelete = null
                        onDeleteNote(item, ::report)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.note_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.note_cancel)) }
            },
        )
    }
}

@Composable
private fun Summary(items: List<PlannerItem>, groups: List<Pair<Planner.Group, List<PlannerItem>>>) {
    val today = groups.firstOrNull { it.first == Planner.Group.TODAY }?.second?.size ?: 0
    val overdue = groups.firstOrNull { it.first == Planner.Group.OVERDUE }?.second?.size ?: 0
    Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
        Text(
            if (today == 0) stringResource(R.string.summary_none_today) else stringResource(R.string.summary_today, today),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        val parts = buildList {
            add(stringResource(R.string.summary_total, items.size))
            if (overdue > 0) add(stringResource(R.string.summary_overdue, overdue))
        }
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = if (overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Empty(days: Int) {
    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.empty_body, days),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionHead(group: Planner.Group, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp, start = 4.dp, end = 4.dp)) {
        Text(
            stringResource(Labels.group(group)),
            style = MaterialTheme.typography.labelLarge,
            color = if (group == Planner.Group.OVERDUE) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f),
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ItemCard(
    item: PlannerItem,
    now: ZonedDateTime,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val label = Planner.dueLabel(item.due, now)
    val course = Labels.course(context, item)
    val dot = COURSE_PALETTE[courseColorIndex(course, COURSE_PALETTE.size)]

    Card(
        onClick = onToggle,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.padding(top = 7.dp).size(8.dp).clip(CircleShape).background(dot))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.title ?: stringResource(R.string.untitled),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        Labels.meta(context, item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    Labels.due(context, label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (label.isLate || label.isSoon) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        label.isLate -> MaterialTheme.colorScheme.error
                        label.isSoon -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 18.dp, top = 8.dp)) {
                    Text(
                        Labels.fullDue(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!item.details.isNullOrBlank()) {
                        Text(
                            item.details,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                    Row {
                        item.url?.let { url ->
                            TextButton(onClick = { onOpen(url) }) { Text(stringResource(R.string.open_in_canvas)) }
                        }
                        if (item.isNote) {
                            TextButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) { Text(stringResource(R.string.note_delete)) }
                        }
                    }
                }
            }
        }
    }
}
