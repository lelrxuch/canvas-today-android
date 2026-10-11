package gt.cris.canvastoday.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CanvasTodayTheme {
                AppRoot(vm, ::openUrl)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Al volver a la app, la lista se muestra desde la caché y se actualiza por detrás.
        if (vm.state.value.configured) vm.refresh()
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: android.content.ActivityNotFoundException) {
            // Sin navegador ni app de Canvas: no hay nada que abrir.
        }
    }
}

private enum class Screen { LIST, SETTINGS, SETUP }

@Composable
private fun AppRoot(vm: MainViewModel, openUrl: (String) -> Unit) {
    val state by vm.state.collectAsState()
    var screen by rememberSaveable { mutableStateOf(Screen.LIST) }

    if (!state.configured) {
        SetupScreen(initialDomain = state.baseUrl, hasToken = false, onSave = vm::saveSetup, onBack = null)
        return
    }

    NotificationPermission()

    when (screen) {
        Screen.LIST -> ListScreen(
            state = state,
            onRefresh = vm::refresh,
            onSettings = { screen = Screen.SETTINGS },
            onOpen = openUrl,
            onCreateNote = vm::createNote,
            onDeleteNote = vm::deleteNote,
        )
        Screen.SETTINGS -> {
            BackHandler { screen = Screen.LIST }
            SettingsScreen(
                state = state,
                onBack = { screen = Screen.LIST },
                onNotifyHours = vm::setNotifyHours,
                onDaysAhead = vm::setDaysAhead,
                onChangeAccount = { screen = Screen.SETUP },
                onSignOut = {
                    screen = Screen.LIST
                    vm.signOut()
                },
            )
        }
        Screen.SETUP -> {
            BackHandler { screen = Screen.SETTINGS }
            SetupScreen(
                initialDomain = state.baseUrl,
                hasToken = true,
                onSave = { domain, token ->
                    vm.saveSetup(domain, token).also { if (it == null) screen = Screen.LIST }
                },
                onBack = { screen = Screen.SETTINGS },
            )
        }
    }
}

/** Android 13+ pide permiso para avisar; se pide una vez, al entrar a la lista. */
@Composable
private fun NotificationPermission() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
