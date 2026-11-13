package aletheia.archive.ui

import aletheia.archive.data.Message
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ReadingScreen(
    title: String,
    messages: List<Message>,
    draft: String,
    writing: Boolean,
    copiedMessageId: Long?,
    streamingId: Long?,
    streamingText: String?,
    onDraft: (String) -> Unit,
    onSend: () -> Unit,
    onBack: () -> Unit,
    onCopyMessage: (Message, String) -> Unit,
    onCopyAll: () -> Unit,
    onDiscard: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val lastLength = messages.lastOrNull()?.let { shownText(it, streamingId, streamingText).length } ?: 0

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(lastLength) {
        if (writing && messages.isNotEmpty()) listState.scrollToItem(messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onBack) {
                Text("Archive", style = Label)
            }
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = HerBody.copy(color = AletheiaColor.Paper, fontSize = 18.sp, lineHeight = 22.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box {
                TextButton(onClick = { menu = true }) {
                    Text("More", style = Label)
                }
                DropdownMenu(
                    expanded = menu,
                    onDismissRequest = { menu = false },
                    containerColor = AletheiaColor.InkRaised,
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy her replies", style = YourBody) },
                        onClick = {
                            menu = false
                            onCopyAll()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Discard this reading", style = YourBody.copy(color = AletheiaColor.Seal)) },
                        onClick = {
                            menu = false
                            confirm = true
                        },
                    )
                }
            }
        }
        if (messages.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Aletheia", style = Wordmark.copy(fontSize = 32.sp))
                Text(
                    "Write to her. Anything she answers can be copied off the page.",
                    style = Meta,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp),
            ) {
                items(messages, key = { it.id }) { message ->
                    val text = shownText(message, streamingId, streamingText)
                    if (message.role == "assistant") {
                        if (message.pending && text.isEmpty()) {
                            Text(
                                "She is writing",
                                style = HerBody.copy(color = AletheiaColor.Brass, fontSize = 16.sp),
                                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp),
                            )
                        } else if (text.isNotEmpty()) {
                            HerPage(
                                text = text,
                                copied = copiedMessageId == message.id,
                                onCopy = { onCopyMessage(message, text) },
                            )
                        }
                    } else if (text.isNotEmpty()) {
                        YourLine(text)
                    }
                }
            }
        }
        TextField(
            value = draft,
            onValueChange = onDraft,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            textStyle = YourBody,
            placeholder = { Text("Write", style = YourBody.copy(color = AletheiaColor.Faded)) },
            minLines = 1,
            maxLines = 6,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { if (!writing) onSend() }),
            colors = inkFieldColors(),
        )
        TextButton(
            onClick = onSend,
            enabled = !writing && draft.isNotBlank(),
            modifier = Modifier
                .align(Alignment.End)
                .padding(end = 8.dp, bottom = 4.dp)
                .heightIn(min = 48.dp),
            colors = ButtonDefaults.textButtonColors(
                contentColor = AletheiaColor.Brass,
                disabledContentColor = AletheiaColor.Faded,
            ),
        ) {
            Text(if (writing) "Writing" else "Send", style = Label.copy(fontSize = 14.sp))
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = AletheiaColor.InkRaised,
            title = {
                Text("Discard this reading?", style = HerBody.copy(color = AletheiaColor.Paper, fontSize = 22.sp))
            },
            text = {
                Text(
                    "Her replies leave the archive. Passages you copied stay in the folio.",
                    style = YourBody,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onDiscard()
                }) {
                    Text("Discard", color = AletheiaColor.Seal, fontFamily = Outfit, fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) {
                    Text("Keep", color = AletheiaColor.Brass, fontFamily = Outfit)
                }
            },
        )
    }
}

@Composable
fun HerPage(text: String, copied: Boolean, onCopy: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .background(AletheiaColor.Paper),
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .width(2.dp)
                .fillMaxHeight()
                .background(AletheiaColor.Brass),
        )
        Column(
            Modifier.padding(start = 18.dp, end = 12.dp, top = 16.dp, bottom = 4.dp),
        ) {
            SelectionContainer {
                Text(text, style = HerBody)
            }
            TextButton(
                onClick = onCopy,
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = AletheiaColor.BrassInk),
            ) {
                Text(if (copied) "Copied" else "Copy", style = Label.copy(color = AletheiaColor.BrassInk))
            }
        }
    }
}

@Composable
private fun YourLine(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 72.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.End,
    ) {
        Text("YOU", style = Label.copy(color = AletheiaColor.Faded, fontSize = 11.sp))
        SelectionContainer {
            Text(text, style = YourBody, textAlign = TextAlign.End)
        }
    }
}

fun shownText(message: Message, streamingId: Long?, streamingText: String?): String {
    if (message.id == streamingId && streamingText != null) return streamingText
    return message.text
}

@Preview(showBackground = true, backgroundColor = 0xFF100E0C, widthDp = 390, heightDp = 760)
@Composable
private fun ReadingPreview() {
    AletheiaTheme {
        Box(Modifier.background(AletheiaColor.Ink)) {
            ReadingScreen(
                title = "What is a copied sentence",
                messages = listOf(
                    Message(1, 1, "user", "Say one sentence I can carry out of the room.", false, 0),
                    Message(
                        2,
                        1,
                        "assistant",
                        "A copied sentence has to stand without the room it was written in.",
                        false,
                        1,
                    ),
                ),
                draft = "",
                writing = false,
                copiedMessageId = null,
                streamingId = null,
                streamingText = null,
                onDraft = {},
                onSend = {},
                onBack = {},
                onCopyMessage = { _, _ -> },
                onCopyAll = {},
                onDiscard = {},
            )
        }
    }
}
