package aletheia.archive.ui

import aletheia.archive.ArchiveViewModel
import aletheia.archive.Screen
import aletheia.archive.voice.CopyText
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ArchiveRoot(viewModel: ArchiveViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    BackHandler(enabled = state.canBack) { viewModel.back() }

    fun copyOut(text: String): Boolean {
        val passage = CopyText.passage(text)
        if (passage.isEmpty()) return false
        val manager = context.getSystemService(ClipboardManager::class.java)
        manager.setPrimaryClip(ClipData.newPlainText("Aletheia", passage))
        view.confirmCopy()
        return true
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AletheiaColor.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .windowInsetsPadding(
                WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom),
            ),
    ) {
        Column(Modifier.weight(1f).fillMaxSize()) {
            when (val screen = state.screen) {
                Screen.Archive -> ArchiveScreen(
                    rows = state.rows,
                    apiKeySet = state.apiKeySet,
                    onOpen = viewModel::openReading,
                    onNew = viewModel::newReading,
                    onFolio = viewModel::openFolio,
                    onAccount = viewModel::openAccount,
                )
                is Screen.Reading -> {
                    val title = state.rows.find { it.id == screen.conversationId }?.title ?: "New reading"
                    ReadingScreen(
                        title = title,
                        messages = state.messages,
                        draft = state.draft,
                        writing = state.sendingConversationId != null,
                        copiedMessageId = state.copiedMessageId,
                        streamingId = state.streamingId,
                        streamingText = state.streamingText,
                        onDraft = viewModel::draft,
                        onSend = viewModel::send,
                        onBack = viewModel::back,
                        onCopyMessage = { message, shown ->
                            if (!copyOut(shown)) {
                                viewModel.notice("There is nothing on the page to copy.")
                            } else {
                                viewModel.keepPassage(screen.conversationId, message.id, title, shown)
                            }
                        },
                        onCopyAll = {
                            val joined = CopyText.herReplies(
                                state.messages.filter { it.role == "assistant" }.map {
                                    shownText(it, state.streamingId, state.streamingText)
                                },
                            )
                            if (!copyOut(joined)) {
                                viewModel.notice("She has not written anything here yet.")
                            } else {
                                viewModel.keepPassage(screen.conversationId, 0L, title, joined)
                                viewModel.notice("Copied her replies.")
                            }
                        },
                        onDiscard = { viewModel.discardReading(screen.conversationId) },
                    )
                }
                Screen.Folio -> FolioScreen(
                    passages = state.passages,
                    copiedPassageId = state.copiedPassageId,
                    onBack = viewModel::back,
                    onCopy = { passage ->
                        if (copyOut(passage.text)) viewModel.markPassageCopied(passage.id)
                    },
                    onRemove = { viewModel.removePassage(it.id) },
                )
                Screen.Account -> AccountScreen(
                    apiKey = state.formKey,
                    model = state.formModel,
                    language = state.formLanguage,
                    choices = state.modelChoices,
                    loadingModels = state.loadingModels,
                    keyStored = state.apiKeySet,
                    onKey = viewModel::formKey,
                    onModel = viewModel::formModel,
                    onLanguage = viewModel::formLanguage,
                    onLookup = viewModel::lookupModels,
                    onSave = viewModel::saveAccount,
                    onRemoveKey = viewModel::removeKey,
                    onBack = viewModel::back,
                )
            }
        }
        val banner = state.error ?: state.notice
        if (banner != null) {
            Text(
                banner,
                style = Meta.copy(color = if (state.error != null) AletheiaColor.Seal else AletheiaColor.Brass),
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable(enabled = state.error != null) { viewModel.dismissError() },
            )
        }
    }
}

private fun View.confirmCopy() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    } else {
        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }
}
