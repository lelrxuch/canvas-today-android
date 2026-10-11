package gt.cris.canvastoday.ui

import android.content.Intent
import android.provider.Settings as AndroidSettings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import gt.cris.canvastoday.R
import gt.cris.canvastoday.data.Settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onNotifyHours: (Int) -> Unit,
    onDaysAhead: (Int) -> Unit,
    onChangeAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.settings_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Section(stringResource(R.string.settings_notify))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (h in Settings.NOTIFY_OPTIONS) {
                    FilterChip(
                        selected = state.notifyHours == h,
                        onClick = { onNotifyHours(h) },
                        label = { Text(stringResource(R.string.settings_hours_value, h)) },
                    )
                }
            }

            Section(stringResource(R.string.settings_days))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (d in Settings.DAYS_OPTIONS) {
                    FilterChip(
                        selected = state.daysAhead == d,
                        onClick = { onDaysAhead(d) },
                        label = { Text(stringResource(R.string.settings_days_value, d)) },
                    )
                }
            }

            HorizontalDivider()
            Section(stringResource(R.string.settings_battery))
            Text(
                stringResource(R.string.settings_battery_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                },
            ) { Text(stringResource(R.string.settings_battery_open)) }

            HorizontalDivider()
            Section(stringResource(R.string.settings_account))
            Text(state.baseUrl?.removePrefix("https://").orEmpty(), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onChangeAccount) { Text(stringResource(R.string.settings_change)) }
            TextButton(
                onClick = onSignOut,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.settings_logout)) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}
