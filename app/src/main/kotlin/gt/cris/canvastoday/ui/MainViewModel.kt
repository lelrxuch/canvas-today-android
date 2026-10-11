package gt.cris.canvastoday.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import gt.cris.canvastoday.R
import gt.cris.canvastoday.core.NoteDraft
import gt.cris.canvastoday.core.PlannerItem
import gt.cris.canvastoday.core.isTokenFormat
import gt.cris.canvastoday.core.normalizeBaseUrl
import gt.cris.canvastoday.data.Cache
import gt.cris.canvastoday.data.CanvasException
import gt.cris.canvastoday.data.Repository
import gt.cris.canvastoday.data.Settings
import gt.cris.canvastoday.data.Snapshot
import gt.cris.canvastoday.data.TokenStore
import gt.cris.canvastoday.work.Reminders
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiState(
    val configured: Boolean,
    val snapshot: Snapshot,
    val refreshing: Boolean = false,
    val baseUrl: String? = null,
    val notifyHours: Int = Settings.DEFAULT_NOTIFY_HOURS,
    val daysAhead: Int = Settings.DEFAULT_DAYS_AHEAD,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val context get() = getApplication<Application>()
    private val settings = Settings(app)

    private val _state = MutableStateFlow(
        UiState(
            configured = Repository.isConfigured(app),
            snapshot = Cache.load(app),
            baseUrl = settings.baseUrl,
            notifyHours = settings.notifyHours,
            daysAhead = settings.daysAhead,
        ),
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        if (_state.value.configured) refresh()
    }

    fun refresh() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        viewModelScope.launch {
            val snapshot = Repository.sync(context)
            _state.update { it.copy(snapshot = snapshot, refreshing = false) }
        }
    }

    /** Guarda dirección y token; devuelve el id del texto de error o null si todo está bien. */
    fun saveSetup(domain: String, token: String): Int? {
        val base = normalizeBaseUrl(domain) ?: return R.string.setup_error_domain
        val keepToken = token.isBlank() && TokenStore.load(context) != null
        if (!keepToken && !isTokenFormat(token)) return R.string.setup_error_token
        settings.baseUrl = base
        if (!keepToken) TokenStore.save(context, token)
        _state.update { it.copy(configured = true, baseUrl = base) }
        refresh()
        return null
    }

    fun setNotifyHours(hours: Int) {
        settings.notifyHours = hours
        _state.update { it.copy(notifyHours = hours) }
        Reminders.schedule(context, _state.value.snapshot.items, hours)
    }

    fun setDaysAhead(days: Int) {
        settings.daysAhead = days
        _state.update { it.copy(daysAhead = days) }
        refresh()
    }

    fun signOut() {
        viewModelScope.launch {
            Repository.signOut(context)
            _state.update { it.copy(configured = false, snapshot = Snapshot()) }
        }
    }

    /** Crea la nota; `done` recibe null si salió bien o el id del texto de error. */
    fun createNote(draft: NoteDraft, done: (Int?) -> Unit) {
        when (draft.problem()) {
            NoteDraft.Problem.TITLE_EMPTY -> return done(R.string.note_error_title)
            NoteDraft.Problem.TITLE_LONG -> return done(R.string.note_error_title_long)
            NoteDraft.Problem.DETAILS_LONG -> return done(R.string.note_error_details_long)
            null -> Unit
        }
        viewModelScope.launch {
            try {
                val snapshot = Repository.createNote(context, draft)
                _state.update { it.copy(snapshot = snapshot) }
                done(null)
            } catch (e: CanvasException) {
                done(Labels.error(e.kind))
            }
        }
    }

    fun deleteNote(item: PlannerItem, done: (Int?) -> Unit) {
        viewModelScope.launch {
            try {
                val snapshot = Repository.deleteNote(context, item)
                _state.update { it.copy(snapshot = snapshot) }
                done(null)
            } catch (e: CanvasException) {
                done(Labels.error(e.kind))
            }
        }
    }
}
