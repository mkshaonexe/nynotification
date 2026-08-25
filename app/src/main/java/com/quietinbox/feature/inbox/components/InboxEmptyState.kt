package com.quietinbox.feature.inbox.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietinbox.feature.inbox.R

/**
 * Empty state types for the Inbox.
 */
sealed interface InboxEmptyReason {
    data object NoAccess : InboxEmptyReason
    data object NoDataCaptured : InboxEmptyReason
    data class NoSearchResults(val query: String) : InboxEmptyReason
    data object NoFilterMatches : InboxEmptyReason
}

/**
 * Standard empty state component with distinct copy, icon, and call-to-action per reason.
 */
@Composable
fun InboxEmptyState(
    reason: InboxEmptyReason,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon: ImageVector
    val title: String
    val description: String
    val actionLabel: String?

    when (reason) {
        is InboxEmptyReason.NoAccess -> {
            icon = Icons.Filled.NotificationsOff
            title = stringResource(R.string.inbox_empty_no_access_title)
            description = stringResource(R.string.inbox_empty_no_access_desc)
            actionLabel = stringResource(R.string.inbox_empty_no_access_action)
        }
        is InboxEmptyReason.NoDataCaptured -> {
            icon = Icons.Filled.Inbox
            title = stringResource(R.string.inbox_empty_no_data_title)
            description = stringResource(R.string.inbox_empty_no_data_desc)
            actionLabel = null
        }
        is InboxEmptyReason.NoSearchResults -> {
            icon = Icons.Filled.SearchOff
            title = stringResource(R.string.inbox_empty_search_title)
            description = stringResource(R.string.inbox_empty_search_desc, reason.query)
            actionLabel = stringResource(R.string.inbox_clear_filters)
        }
        is InboxEmptyReason.NoFilterMatches -> {
            icon = Icons.Filled.FilterListOff
            title = stringResource(R.string.inbox_empty_filter_title)
            description = stringResource(R.string.inbox_empty_filter_desc)
            actionLabel = stringResource(R.string.inbox_clear_filters)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null) {
            Spacer(modifier = Modifier.height(24.dp))
            if (reason is InboxEmptyReason.NoAccess) {
                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            } else {
                OutlinedButton(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
