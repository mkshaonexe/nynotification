package com.quietinbox.feature.inbox

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.quietinbox.R
import com.quietinbox.core.apps.InstalledAppsProvider
import com.quietinbox.core.health.HealthState
import com.quietinbox.feature.inbox.components.InboxAppFilterSheet
import com.quietinbox.feature.inbox.components.InboxDayHeader
import com.quietinbox.feature.inbox.components.InboxDetailSheet
import com.quietinbox.feature.inbox.components.InboxEmptyReason
import com.quietinbox.feature.inbox.components.InboxEmptyState
import com.quietinbox.feature.inbox.components.InboxMultiSelectTopBar
import com.quietinbox.feature.inbox.components.InboxNotificationRow
import com.quietinbox.feature.inbox.components.InboxSessionRow
import com.quietinbox.feature.inbox.model.InboxItem
import com.quietinbox.feature.inbox.model.InboxStateFilter
import kotlinx.coroutines.flow.collectLatest

/**
 * Main Inbox Screen composable.
 *
 * @param onNavigateToPermissions Callback when user taps "Grant permission" from empty state.
 * @param installedAppsProvider Provider for installed app labels and icons.
 * @param viewModel Injected InboxViewModel instance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    onNavigateToPermissions: () -> Unit = {},
    installedAppsProvider: InstalledAppsProvider? = null,
    viewModel: InboxViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val stateFilter by viewModel.stateFilter.collectAsState()
    val selectedPackage by viewModel.selectedPackage.collectAsState()
    val selectedDay by viewModel.selectedDay.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val detailNotification by viewModel.detailNotification.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val healthSnapshot by viewModel.healthSnapshot.collectAsState()
    val totalCount by viewModel.totalNotificationCount.collectAsState()

    val pagedItems = viewModel.pagedInboxItems.collectAsLazyPagingItems()
    val isMultiSelectMode = selectedIds.isNotEmpty()

    var showAppFilterSheet by remember { mutableStateOf(false) }

    // Handle UI events (snackbars, clipboard copy)
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is InboxUiEvent.ShowSnackbar -> {
                    if (event.actionLabel != null) {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    } else {
                        snackbarHostState.showSnackbar(
                            message = event.message,
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                is InboxUiEvent.CopyToClipboard -> {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = ClipData.newPlainText("OTP Code", event.code)
                    clipboard?.setPrimaryClip(clip)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            if (isMultiSelectMode) {
                val currentRows = remember(pagedItems.itemCount, selectedIds) {
                    val list = mutableListOf<InboxItem.Row>()
                    for (i in 0 until pagedItems.itemCount) {
                        val item = pagedItems.peek(i)
                        if (item is InboxItem.Row && selectedIds.contains(item.id)) {
                            list.add(item)
                        }
                    }
                    list
                }

                InboxMultiSelectTopBar(
                    selectedCount = selectedIds.size,
                    onCloseSelection = { viewModel.clearSelection() },
                    onSelectAll = {
                        val allIds = mutableListOf<Long>()
                        for (i in 0 until pagedItems.itemCount) {
                            val item = pagedItems.peek(i)
                            if (item is InboxItem.Row) {
                                allIds.add(item.id)
                            }
                        }
                        viewModel.selectAll(allIds)
                    },
                    onMarkSeen = { viewModel.markSelectedSeen(true) },
                    onToggleStar = { viewModel.toggleStarSelected() },
                    onDelete = { viewModel.deleteSelected(currentRows) },
                    onMuteApp = { viewModel.muteSelectedApps(currentRows) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sticky Search & Filter Header (when not multi-selecting)
            AnimatedVisibility(
                visible = !isMultiSelectMode,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.inbox_search_hint),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.inbox_search_icon),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clearAndSetSemantics {
                                            contentDescription = "Clear search"
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Horizontal Filter Chips Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // State Chips: All, Unseen, Seen, Starred
                        InboxStateFilter.entries.forEach { filter ->
                            val isSelected = stateFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onStateFilterChanged(filter) },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            InboxStateFilter.ALL -> stringResource(R.string.inbox_filter_all)
                                            InboxStateFilter.UNSEEN -> stringResource(R.string.inbox_filter_unseen)
                                            InboxStateFilter.SEEN -> stringResource(R.string.inbox_filter_seen)
                                            InboxStateFilter.STARRED -> stringResource(R.string.inbox_filter_starred)
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.height(48.dp)
                            )
                        }

                        // App Filter Chip
                        val appLabel = remember(selectedPackage, installedApps) {
                            if (selectedPackage == null) null
                            else installedApps.firstOrNull { it.packageName == selectedPackage }?.label ?: selectedPackage
                        }
                        val isAppSelected = selectedPackage != null

                        FilterChip(
                            selected = isAppSelected,
                            onClick = { showAppFilterSheet = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Apps,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                if (isAppSelected) {
                                    IconButton(
                                        onClick = { viewModel.onPackageFilterChanged(null) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Clear app filter",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = if (appLabel != null) "App: $appLabel" else stringResource(R.string.inbox_filter_app),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isAppSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.height(48.dp)
                        )

                        // Day Filter Chip (if active from heatmap deep link)
                        if (selectedDay != null) {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.onDayFilterChanged(null) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear day filter",
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = "Day: $selectedDay",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }
            }

            // Body: List / Empty States
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val isListEmpty = pagedItems.itemCount == 0
                val isRefreshing = pagedItems.loadState.refresh is LoadState.Loading

                if (isRefreshing && isListEmpty) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (healthSnapshot.state == HealthState.NO_ACCESS && isListEmpty) {
                    InboxEmptyState(
                        reason = InboxEmptyReason.NoAccess,
                        onActionClick = onNavigateToPermissions
                    )
                } else if (isListEmpty) {
                    val emptyReason = when {
                        searchQuery.isNotBlank() -> InboxEmptyReason.NoSearchResults(searchQuery)
                        stateFilter != InboxStateFilter.ALL || selectedPackage != null || selectedDay != null -> InboxEmptyReason.NoFilterMatches
                        else -> InboxEmptyReason.NoDataCaptured
                    }
                    InboxEmptyState(
                        reason = emptyReason,
                        onActionClick = { viewModel.clearFilters() }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        for (index in 0 until pagedItems.itemCount) {
                            val item = pagedItems.peek(index)
                            if (item is InboxItem.Header) {
                                stickyHeader(key = "header_${item.epochDay}_$index") {
                                    InboxDayHeader(title = item.title)
                                }
                            } else if (item is InboxItem.Row) {
                                item(key = item.id) {
                                    val isSelected = selectedIds.contains(item.id)
                                    val iconFile = installedAppsProvider?.iconFile(item.packageName)

                                    if (item.isSession) {
                                        InboxSessionRow(
                                            item = item,
                                            searchQuery = searchQuery,
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = isSelected,
                                            iconFile = iconFile,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    viewModel.toggleSelect(item.id)
                                                } else {
                                                    viewModel.openDetail(item)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelect(item.id)
                                            },
                                            onToggleStar = {
                                                viewModel.toggleStar(item.id, item.isStarred)
                                            },
                                            onDelete = {
                                                viewModel.deleteNotification(item)
                                            }
                                        )
                                    } else {
                                        InboxNotificationRow(
                                            item = item,
                                            searchQuery = searchQuery,
                                            isMultiSelectMode = isMultiSelectMode,
                                            isSelected = isSelected,
                                            iconFile = iconFile,
                                            onClick = {
                                                if (isMultiSelectMode) {
                                                    viewModel.toggleSelect(item.id)
                                                } else {
                                                    viewModel.openDetail(item)
                                                }
                                            },
                                            onLongClick = {
                                                viewModel.toggleSelect(item.id)
                                            },
                                            onToggleStar = {
                                                viewModel.toggleStar(item.id, item.isStarred)
                                            },
                                            onDelete = {
                                                viewModel.deleteNotification(item)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    detailNotification?.let { notification ->
        val iconFile = installedAppsProvider?.iconFile(notification.packageName)
        InboxDetailSheet(
            notification = notification,
            iconFile = iconFile,
            onDismiss = { viewModel.closeDetail() },
            onCopyCode = { code -> viewModel.copyCode(code) },
            onMuteApp = { pkg -> viewModel.muteApp(pkg) },
            onAlwaysAllowSender = { senderName, senderDigits, pkg ->
                viewModel.alwaysAllowSender(senderName, senderDigits, pkg)
            }
        )
    }

    // App Filter Bottom Sheet
    if (showAppFilterSheet && installedAppsProvider != null) {
        InboxAppFilterSheet(
            installedApps = installedApps,
            selectedPackage = selectedPackage,
            installedAppsProvider = installedAppsProvider,
            onAppSelected = { pkg -> viewModel.onPackageFilterChanged(pkg) },
            onDismiss = { showAppFilterSheet = false }
        )
    }
}
