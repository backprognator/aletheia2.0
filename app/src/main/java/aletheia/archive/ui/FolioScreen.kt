package aletheia.archive.ui

import aletheia.archive.data.Passage
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FolioScreen(
    passages: List<Passage>,
    copiedPassageId: Long?,
    onBack: () -> Unit,
    onCopy: (Passage) -> Unit,
    onRemove: (Passage) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Text("Archive", style = Label)
        }
        Text("Folio", style = Wordmark.copy(fontSize = 34.sp), modifier = Modifier.padding(horizontal = 20.dp))
        Text(
            "Passages you copied.",
            style = Meta,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        if (passages.isEmpty()) {
            Text(
                "Nothing copied yet. On her page, Copy puts a passage on the clipboard and keeps it here.",
                style = HerBody.copy(color = AletheiaColor.Parchment, fontSize = 18.sp, lineHeight = 26.sp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(passages, key = { it.id }) { passage ->
                    Box(
                        Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .background(AletheiaColor.Paper),
                    ) {
                        Box(
                            Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxHeight()
                                .width(2.dp)
                                .background(AletheiaColor.Brass),
                        )
                        Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
                            Text(
                                "${passage.sourceTitle} · ${formatWhen(passage.copiedAt)}",
                                style = Meta.copy(color = AletheiaColor.BrassInk),
                            )
                            SelectionContainer {
                                Text(
                                    passage.text,
                                    style = HerBody.copy(fontSize = 16.sp, lineHeight = 24.sp),
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                            Row {
                                TextButton(
                                    onClick = { onCopy(passage) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    colors = ButtonDefaults.textButtonColors(contentColor = AletheiaColor.BrassInk),
                                ) {
                                    Text(
                                        if (copiedPassageId == passage.id) "Copied" else "Copy",
                                        style = Label.copy(color = AletheiaColor.BrassInk),
                                    )
                                }
                                TextButton(
                                    onClick = { onRemove(passage) },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                ) {
                                    Text("Remove", style = Label.copy(color = AletheiaColor.Faded))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
