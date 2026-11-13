package aletheia.archive.ui

import aletheia.archive.voice.Languages
import aletheia.wire.ModelEntry
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccountScreen(
    apiKey: String,
    model: String,
    language: String,
    choices: List<ModelEntry>,
    loadingModels: Boolean,
    keyStored: Boolean,
    onKey: (String) -> Unit,
    onModel: (String) -> Unit,
    onLanguage: (String) -> Unit,
    onLookup: () -> Unit,
    onSave: () -> Unit,
    onRemoveKey: () -> Unit,
    onBack: () -> Unit,
) {
    var hidden by remember { mutableStateOf(true) }
    var languagesOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val query = model.trim()
    val shown = choices.filter { entry ->
        query.isEmpty() ||
            entry.id.contains(query, ignoreCase = true) ||
            entry.name.contains(query, ignoreCase = true)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        TextButton(onClick = onBack, modifier = Modifier.padding(start = 0.dp)) {
            Text("Archive", style = Label)
        }
        Text("Account", style = Wordmark.copy(fontSize = 34.sp), modifier = Modifier.padding(top = 4.dp))
        Text(
            "The key stays on this device. Aletheia sends it only to OpenRouter, as a bearer token, when you ask her something.",
            style = Meta,
            modifier = Modifier.padding(top = 8.dp, bottom = 18.dp),
        )
        Text("OPENROUTER KEY", style = Label)
        TextField(
            value = apiKey,
            onValueChange = onKey,
            modifier = Modifier.fillMaxWidth(),
            textStyle = YourBody,
            singleLine = true,
            visualTransformation = if (hidden) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            placeholder = { Text("sk-or-…", style = YourBody.copy(color = AletheiaColor.Faded)) },
            colors = inkFieldColors(),
        )
        TextButton(onClick = { hidden = !hidden }) {
            Text(if (hidden) "Show key" else "Hide key", style = Label)
        }
        TextButton(onClick = {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://openrouter.ai/keys")))
        }) {
            Text("Open OpenRouter keys", style = Label)
        }

        Text("LANGUAGE", style = Label, modifier = Modifier.padding(top = 12.dp))
        Box {
            TextButton(onClick = { languagesOpen = true }) {
                Text(language.ifBlank { "English" }, style = YourBody.copy(color = AletheiaColor.Paper))
            }
            DropdownMenu(
                expanded = languagesOpen,
                onDismissRequest = { languagesOpen = false },
                containerColor = AletheiaColor.InkRaised,
            ) {
                Languages.all.forEach { tongue ->
                    DropdownMenuItem(
                        text = { Text(tongue, style = YourBody) },
                        onClick = {
                            languagesOpen = false
                            onLanguage(tongue)
                        },
                    )
                }
            }
        }

        Text("MODEL", style = Label, modifier = Modifier.padding(top = 8.dp))
        Text(
            "Leave this as openrouter/auto, or name a provider/model. A blank field uses your OpenRouter default.",
            style = Meta,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
        )
        TextField(
            value = model,
            onValueChange = onModel,
            modifier = Modifier.fillMaxWidth(),
            textStyle = YourBody,
            singleLine = true,
            placeholder = { Text("openrouter/auto", style = YourBody.copy(color = AletheiaColor.Faded)) },
            colors = inkFieldColors(),
        )
        TextButton(onClick = onLookup, enabled = !loadingModels) {
            Text(if (loadingModels) "Looking up models" else "Look up models", style = Label)
        }
        if (choices.isNotEmpty()) {
            Text(
                if (shown.size < choices.size) {
                    "Showing ${shown.take(40).size} matches. Keep typing."
                } else {
                    "Tap a model to use it."
                },
                style = Meta,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        shown.take(40).forEach { entry ->
            TextButton(
                onClick = { onModel(entry.id) },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Text(entry.id, style = YourBody.copy(color = AletheiaColor.Paper, fontSize = 14.sp))
                    if (entry.name != entry.id) {
                        Text(entry.name, style = Meta)
                    }
                }
            }
        }

        TextButton(
            onClick = onSave,
            modifier = Modifier.padding(top = 12.dp),
            colors = ButtonDefaults.textButtonColors(contentColor = AletheiaColor.Brass),
        ) {
            Text("Save", style = Label.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
        if (keyStored) {
            TextButton(onClick = onRemoveKey) {
                Text("Remove key", style = Label.copy(color = AletheiaColor.Seal))
            }
        }
    }
}
