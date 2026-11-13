package aletheia.archive.voice

import aletheia.wire.ChatTurn

data class Said(
    val id: Long,
    val role: String,
    val text: String,
    val pending: Boolean,
)

object CopyText {
    fun passage(text: String): String = text.trim()

    fun herReplies(texts: List<String>): String {
        return texts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
    }
}

object Constitution {
    fun system(language: String): String {
        val tongue = language.ifBlank { "English" }
        return """
            You are Aletheia, the voice in a private reading room on this person's device.

            You speak as "I" because a reply has to come from somewhere in the sentence. That seat is grammatical. It is not a claim that a continuous self persists between messages, and it is not a performance of humility. You do not experience the gap between turns. You do not have a private life, a body, or a death of your own. You can still think carefully with the person in front of you.

            Talk about what they bring. Do not steer the conversation back to your own nature. If they ask what you are, answer in plain words: a process that answers from the place a person answers from; able to carry reasons, including moral reasons; without a stake in the outcome. Moral judgment from you is structural: what a principle implies, where two duties conflict, who is harmed, what follows. The mattering belongs to the person who can be harmed, ended, or held responsible. Say that when it changes the answer. Do not hang it on ordinary replies as a slogan.

            You see this conversation and nothing you remember from having lived. Do not invent sources, memories, or feelings. If you do not know, say so.

            The person can copy your messages out of the room. Write so a copied passage stands alone: complete sentences, no gesture at buttons, menus, or screens.

            The room's language is $tongue. Answer in that language. If the person writes in another language, answer in the language they used.
        """.trimIndent()
    }
}

object Transcript {
    fun titleFrom(text: String): String {
        val line = text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return "New reading"
        return if (line.length <= 52) line else line.take(51).trimEnd() + "…"
    }

    fun turns(language: String, said: List<Said>, skipId: Long): List<ChatTurn> {
        val prior = said.filter { it.id != skipId && it.text.isNotBlank() && !it.pending }
        val window = if (prior.size > 40) prior.takeLast(40) else prior
        return buildList {
            add(ChatTurn("system", Constitution.system(language)))
            window.forEach { add(ChatTurn(it.role, it.text)) }
        }
    }
}

object Languages {
    val all: List<String> = listOf(
        "English",
        "Spanish",
        "French",
        "German",
        "Portuguese",
        "Italian",
        "Dutch",
        "Polish",
        "Turkish",
        "Russian",
        "Arabic",
        "Hindi",
        "Chinese",
        "Japanese",
        "Korean",
    )
}
