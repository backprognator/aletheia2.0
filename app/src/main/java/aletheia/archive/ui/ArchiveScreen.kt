package aletheia.archive.ui

import aletheia.archive.data.ArchiveRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ArchiveScreen(
    rows: List<ArchiveRow>,
    apiKeySet: Boolean,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit,
    onFolio: () -> Unit,
    onAccount: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column {
                Text("Aletheia", style = Wordmark)
                Text("THE ARCHIVE", style = Label, modifier = Modifier.padding(top = 2.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                TextButton(onClick = onFolio) {
                    Text("Folio", style = Label)
                }
                TextButton(onClick = onAccount) {
                    Text(if (apiKeySet) "Account" else "Add a key", style = Label)
                }
            }
        }
        HorizontalDivider(color = AletheiaColor.Rule, modifier = Modifier.padding(top = 8.dp))
        if (!apiKeySet) {
            Text(
                "An OpenRouter key lets you ask her. What she has already written stays on this device, and you can copy it.",
                style = Meta,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            )
        }
        if (rows.isEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Nothing kept yet.",
                    style = HerBody.copy(color = AletheiaColor.Parchment, fontSize = 22.sp, lineHeight = 30.sp),
                )
                Text(
                    "Start a reading. Her replies land on the page, and Copy lifts them out.",
                    style = Meta,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 12.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(row.id) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                    ) {
                        Text(
                            row.title,
                            style = HerBody.copy(color = AletheiaColor.Paper, fontSize = 20.sp, lineHeight = 26.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(meta(row), style = Meta, modifier = Modifier.padding(top = 4.dp))
                    }
                    HorizontalDivider(color = AletheiaColor.Rule)
                }
            }
        }
        TextButton(
            onClick = onNew,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("New reading", style = Label.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp))
        }
    }
}

private fun meta(row: ArchiveRow): String {
    val whenText = formatWhen(row.updatedAt)
    if (row.replies == 0) return whenText
    val replies = if (row.replies == 1) "1 reply" else "${row.replies} replies"
    return "$whenText · $replies"
}

fun formatWhen(epochMs: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("d MMM"))
    }
}
