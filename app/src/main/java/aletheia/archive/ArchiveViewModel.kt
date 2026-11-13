@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package aletheia.archive

import aletheia.archive.data.ArchiveDao
import aletheia.archive.data.Conversation
import aletheia.archive.data.Gate
import aletheia.archive.data.Message
import aletheia.archive.data.Passage
import aletheia.archive.data.SettingsStore
import aletheia.archive.voice.CopyText
import aletheia.archive.voice.Said
import aletheia.archive.voice.Transcript
import aletheia.wire.ModelEntry
import aletheia.wire.OpenRouterClient
import aletheia.wire.OpenRouterException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import java.io.IOException
import java.util.ArrayDeque
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.runInterruptible

sealed interface Screen {
    data object Archive : Screen
    data class Reading(val conversationId: Long) : Screen
    data object Folio : Screen
    data object Account : Screen
}

data class ArchiveUi(
    val screen: Screen = Screen.Archive,
    val canBack: Boolean = false,
    val apiKeySet: Boolean = false,
    val model: String = SettingsStore.DEFAULT_MODEL,
    val language: String = SettingsStore.DEFAULT_LANGUAGE,
    val rows: List<aletheia.archive.data.ArchiveRow> = emptyList(),
    val messages: List<Message> = emptyList(),
    val passages: List<Passage> = emptyList(),
    val draft: String = "",
    val sendingConversationId: Long? = null,
    val streamingId: Long? = null,
    val streamingText: String? = null,
    val error: String? = null,
    val notice: String? = null,
    val copiedMessageId: Long? = null,
    val copiedPassageId: Long? = null,
    val formKey: String = "",
    val formModel: String = SettingsStore.DEFAULT_MODEL,
    val formLanguage: String = SettingsStore.DEFAULT_LANGUAGE,
    val modelChoices: List<ModelEntry> = emptyList(),
    val loadingModels: Boolean = false,
)

class ArchiveViewModel(
    private val dao: ArchiveDao,
    private val settings: SettingsStore,
    private val openRouter: OpenRouterClient,
) : ViewModel() {
    private val stack = ArrayDeque<Screen>().apply { add(Screen.Archive) }
    private val openId = MutableStateFlow<Long?>(null)
    private val _state = MutableStateFlow(ArchiveUi())
    val state: StateFlow<ArchiveUi> = _state.asStateFlow()

    private var streamJob: Job? = null
    private var noticeJob: Job? = null
    private var copiedJob: Job? = null

    init {
        val gate = settings.read()
        _state.update {
            it.copy(
                apiKeySet = gate.apiKey.isNotBlank(),
                model = gate.model.ifBlank { SettingsStore.DEFAULT_MODEL },
                language = gate.language,
            )
        }
        viewModelScope.launch {
            dao.archive().collect { rows -> _state.update { it.copy(rows = rows) } }
        }
        viewModelScope.launch {
            dao.passages().collect { passages -> _state.update { it.copy(passages = passages) } }
        }
        viewModelScope.launch {
            openId.flatMapLatest { id ->
                if (id == null) flowOf(emptyList()) else dao.messages(id)
            }.collect { list ->
                _state.update { current ->
                    val caughtUp = current.sendingConversationId == null &&
                        current.streamingId != null &&
                        list.any { it.id == current.streamingId && it.text == current.streamingText }
                    if (caughtUp) {
                        current.copy(messages = list, streamingId = null, streamingText = null)
                    } else {
                        current.copy(messages = list)
                    }
                }
            }
        }
    }

    fun openArchive() = push(Screen.Archive)

    fun openFolio() = push(Screen.Folio)

    fun openReading(id: Long) {
        openId.value = id
        push(Screen.Reading(id))
        _state.update { it.copy(draft = "", error = null, notice = null) }
    }

    fun newReading() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val id = dao.insertConversation(
                Conversation(title = "New reading", createdAt = now, updatedAt = now),
            )
            openReading(id)
        }
    }

    fun openAccount() {
        val gate = settings.read()
        push(Screen.Account)
        _state.update {
            it.copy(
                formKey = gate.apiKey,
                formModel = gate.model,
                formLanguage = gate.language,
                modelChoices = emptyList(),
                loadingModels = false,
                error = null,
            )
        }
    }

    fun back() {
        if (stack.size <= 1) return
        val leaving = stack.removeLast()
        val screen = stack.last()
        openId.value = (screen as? Screen.Reading)?.conversationId
        _state.update {
            it.copy(
                screen = screen,
                canBack = stack.size > 1,
                draft = if (leaving is Screen.Reading) "" else it.draft,
                error = null,
            )
        }
    }

    fun draft(value: String) {
        _state.update { it.copy(draft = value) }
    }

    fun formKey(value: String) = _state.update { it.copy(formKey = value) }

    fun formModel(value: String) = _state.update { it.copy(formModel = value) }

    fun formLanguage(value: String) = _state.update { it.copy(formLanguage = value) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun saveAccount() {
        val current = _state.value
        val gate = Gate(
            apiKey = current.formKey.trim(),
            model = current.formModel.trim(),
            language = current.formLanguage.ifBlank { SettingsStore.DEFAULT_LANGUAGE },
        )
        settings.write(gate)
        _state.update {
            it.copy(
                apiKeySet = gate.apiKey.isNotBlank(),
                model = gate.model.ifBlank { SettingsStore.DEFAULT_MODEL },
                language = gate.language,
            )
        }
        notice("Saved.")
        back()
    }

    fun removeKey() {
        val gate = settings.read().copy(apiKey = "")
        settings.write(gate)
        _state.update { it.copy(formKey = "", apiKeySet = false) }
        notice("Key removed.")
    }

    fun lookupModels() {
        val key = _state.value.formKey.trim()
        if (key.isBlank()) {
            _state.update { it.copy(error = "Add the key first.") }
            return
        }
        _state.update { it.copy(loadingModels = true, error = null) }
        viewModelScope.launch {
            try {
                val models = runInterruptible(Dispatchers.IO) { openRouter.models(key) }
                _state.update {
                    it.copy(
                        loadingModels = false,
                        modelChoices = models,
                        error = if (models.isEmpty()) "OpenRouter returned no models." else null,
                    )
                }
            } catch (exception: IOException) {
                _state.update { it.copy(loadingModels = false, error = explain(exception)) }
            }
        }
    }

    fun send() {
        val current = _state.value
        if (current.sendingConversationId != null) return
        val text = current.draft.trim()
        if (text.isEmpty()) return
        val reading = current.screen as? Screen.Reading ?: return
        val gate = settings.read()
        if (gate.apiKey.isBlank()) {
            _state.update { it.copy(error = "Add an OpenRouter key to ask her.") }
            openAccount()
            _state.update { it.copy(error = "Add an OpenRouter key to ask her.") }
            return
        }
        _state.update { it.copy(draft = "", error = null, notice = null, sendingConversationId = reading.conversationId) }
        streamJob = viewModelScope.launch {
            var assistantId = 0L
            val buffer = StringBuilder()
            try {
                val now = System.currentTimeMillis()
                val conversation = dao.conversation(reading.conversationId)
                val title = when {
                    conversation == null -> Transcript.titleFrom(text)
                    conversation.title == "New reading" -> Transcript.titleFrom(text)
                    else -> conversation.title
                }
                dao.touch(reading.conversationId, title, now)
                dao.insertMessage(
                    Message(
                        conversationId = reading.conversationId,
                        role = "user",
                        text = text,
                        pending = false,
                        createdAt = now,
                    ),
                )
                assistantId = dao.insertMessage(
                    Message(
                        conversationId = reading.conversationId,
                        role = "assistant",
                        text = "",
                        pending = true,
                        createdAt = now + 1,
                    ),
                )
                _state.update { it.copy(streamingId = assistantId, streamingText = "") }
                val history = dao.messagesOnce(reading.conversationId).map {
                    Said(it.id, it.role, it.text, it.pending)
                }
                val turns = Transcript.turns(gate.language, history, skipId = assistantId)
                var lastPersist = 0L
                runInterruptible(Dispatchers.IO) {
                    openRouter.stream(gate.apiKey, gate.model, turns) { delta ->
                        buffer.append(delta)
                        val page = buffer.toString()
                        _state.update { it.copy(streamingId = assistantId, streamingText = page) }
                        val clock = System.currentTimeMillis()
                        if (clock - lastPersist > 250) {
                            lastPersist = clock
                            runBlocking { dao.updateMessage(assistantId, page, true) }
                        }
                    }
                }
                val finalText = buffer.toString()
                if (finalText.isBlank()) {
                    dao.deleteMessage(assistantId)
                    _state.update {
                        it.copy(
                            error = "She returned an empty page.",
                            streamingId = null,
                            streamingText = null,
                            sendingConversationId = null,
                        )
                    }
                } else {
                    dao.updateMessage(assistantId, finalText, false)
                    dao.touch(reading.conversationId, title, System.currentTimeMillis())
                    _state.update { currentState ->
                        val caughtUp = currentState.messages.any { it.id == assistantId && it.text == finalText }
                        currentState.copy(
                            sendingConversationId = null,
                            streamingId = if (caughtUp) null else assistantId,
                            streamingText = if (caughtUp) null else finalText,
                        )
                    }
                }
            } catch (exception: kotlinx.coroutines.CancellationException) {
                val partial = buffer.toString()
                if (assistantId != 0L) {
                    if (partial.isBlank()) dao.deleteMessage(assistantId) else dao.updateMessage(assistantId, partial, false)
                }
                _state.update { it.copy(sendingConversationId = null, streamingId = null, streamingText = null) }
                throw exception
            } catch (exception: IOException) {
                val partial = buffer.toString()
                if (assistantId != 0L) {
                    if (partial.isBlank()) dao.deleteMessage(assistantId) else dao.updateMessage(assistantId, partial, false)
                }
                _state.update {
                    it.copy(
                        error = explain(exception),
                        sendingConversationId = null,
                        streamingId = null,
                        streamingText = null,
                    )
                }
            } finally {
                _state.update { it.copy(sendingConversationId = null) }
            }
        }
    }

    fun discardReading(conversationId: Long) {
        if (_state.value.sendingConversationId == conversationId) {
            openRouter.cancel()
            streamJob?.cancel()
        }
        viewModelScope.launch {
            dao.deleteMessages(conversationId)
            dao.deleteConversation(conversationId)
            stack.removeAll { it is Screen.Reading && it.conversationId == conversationId }
            if (stack.isEmpty()) stack.addLast(Screen.Archive)
            val screen = stack.last()
            openId.value = (screen as? Screen.Reading)?.conversationId
            _state.update {
                it.copy(screen = screen, canBack = stack.size > 1, error = null, draft = "")
            }
        }
    }

    fun keepPassage(conversationId: Long, messageId: Long, title: String, text: String) {
        val passage = CopyText.passage(text)
        if (passage.isEmpty()) return
        markCopied(messageId)
        viewModelScope.launch {
            if (dao.passageByText(passage) == null) {
                dao.insertPassage(
                    Passage(
                        conversationId = conversationId,
                        messageId = messageId,
                        sourceTitle = title.ifBlank { "Aletheia" },
                        text = passage,
                        copiedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    fun removePassage(id: Long) {
        viewModelScope.launch { dao.deletePassage(id) }
    }

    fun markPassageCopied(id: Long) {
        copiedJob?.cancel()
        _state.update { it.copy(copiedPassageId = id) }
        copiedJob = viewModelScope.launch {
            delay(1400)
            _state.update { if (it.copiedPassageId == id) it.copy(copiedPassageId = null) else it }
        }
    }

    fun notice(text: String) {
        noticeJob?.cancel()
        _state.update { it.copy(notice = text, error = null) }
        noticeJob = viewModelScope.launch {
            delay(2200)
            _state.update { if (it.notice == text) it.copy(notice = null) else it }
        }
    }

    private fun markCopied(messageId: Long) {
        copiedJob?.cancel()
        _state.update { it.copy(copiedMessageId = messageId) }
        copiedJob = viewModelScope.launch {
            delay(1400)
            _state.update { if (it.copiedMessageId == messageId) it.copy(copiedMessageId = null) else it }
        }
    }

    private fun push(screen: Screen) {
        stack.addLast(screen)
        if (screen is Screen.Reading) openId.value = screen.conversationId
        _state.update { it.copy(screen = screen, canBack = true) }
    }

    private fun explain(exception: IOException): String {
        val message = exception.message.orEmpty()
        return when {
            exception is OpenRouterException && message.isNotBlank() -> message
            message.contains("timeout", ignoreCase = true) -> "OpenRouter did not answer in time."
            message.contains("cancel", ignoreCase = true) -> "The line to OpenRouter closed."
            message.isBlank() -> "Could not reach OpenRouter."
            else -> message
        }
    }

    companion object {
        fun factory(app: AletheiaApp): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(ArchiveViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return ArchiveViewModel(app.database.dao, app.settings, app.openRouter) as T
                    }
                    error("Unknown model $modelClass")
                }
            }
        }
    }
}
