package gt.cris.canvastoday.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gt.cris.canvastoday.R
import gt.cris.canvastoday.core.NoteDraft
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Nueva nota personal: título, detalles, fecha, hora opcional y curso opcional. */
@Composable
fun NoteDialog(
    courses: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (NoteDraft, (Int?) -> Unit) -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var time by remember { mutableStateOf<LocalTime?>(null) }
    var courseId by remember { mutableStateOf<Long?>(null) }
    var courseMenu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<Int?>(null) }

    val dateText = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(date)
    val timeText = time?.let { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(it) }
        ?: stringResource(R.string.note_no_time)
    val courseText = courses.firstOrNull { it.first == courseId }?.second ?: stringResource(R.string.note_no_course)

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text(stringResource(R.string.note_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.note_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = details,
                    onValueChange = { details = it },
                    label = { Text(stringResource(R.string.note_details)) },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> date = LocalDate.of(y, m + 1, d) },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth,
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(dateText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    OutlinedButton(
                        onClick = {
                            val t = time ?: LocalTime.of(8, 0)
                            TimePickerDialog(
                                context,
                                { _, h, min -> time = LocalTime.of(h, min) },
                                t.hour,
                                t.minute,
                                DateFormat.is24HourFormat(context),
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(timeText, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
                if (courses.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { courseMenu = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(courseText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        DropdownMenu(expanded = courseMenu, onDismissRequest = { courseMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.note_no_course)) },
                                onClick = {
                                    courseId = null
                                    courseMenu = false
                                },
                            )
                            for ((id, name) in courses) {
                                DropdownMenuItem(
                                    text = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        courseId = id
                                        courseMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
                Text(
                    stringResource(R.string.note_sync_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    saving = true
                    error = null
                    onSave(NoteDraft(title, details, date, time, courseId)) { e ->
                        saving = false
                        error = e
                    }
                },
            ) { Text(stringResource(R.string.note_save)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onDismiss) { Text(stringResource(R.string.note_cancel)) }
        },
    )
}
