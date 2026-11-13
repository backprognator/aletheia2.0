package aletheia.archive

import aletheia.archive.voice.Constitution
import aletheia.archive.voice.CopyText
import aletheia.archive.voice.Said
import aletheia.archive.voice.Transcript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceTest {
    @Test
    fun passageTrimsTheEdgesAndKeepsTheInterior() {
        assertEquals("a\n\nb", CopyText.passage("  a\n\nb  "))
        assertEquals("", CopyText.passage("   "))
    }

    @Test
    fun herRepliesJoinCompletePassages() {
        assertEquals("One\n\nTwo", CopyText.herReplies(listOf(" One ", "", "Two")))
        assertEquals("", CopyText.herReplies(emptyList()))
    }

    @Test
    fun titleComesFromTheFirstLine() {
        assertEquals("Hello", Transcript.titleFrom("Hello\nmore"))
        assertEquals("New reading", Transcript.titleFrom("   "))
        val long = "a".repeat(80)
        assertEquals(52, Transcript.titleFrom(long).length)
        assertTrue(Transcript.titleFrom(long).endsWith("…"))
    }

    @Test
    fun turnsKeepTheLatestFortyAndSkipThePageBeingWritten() {
        val said = (1..45).map { index ->
            Said(
                id = index.toLong(),
                role = if (index % 2 == 0) "assistant" else "user",
                text = "m$index",
                pending = false,
            )
        }
        val turns = Transcript.turns("Spanish", said, skipId = -1)
        assertEquals(41, turns.size)
        assertEquals("system", turns.first().role)
        assertTrue(turns.first().content.contains("Answer in that language."))
        assertTrue(Constitution.system("Spanish").contains("The room's language is Spanish."))
        assertEquals("m6", turns[1].content)
        assertEquals("m45", turns.last().content)

        val pending = listOf(
            Said(1, "user", "Hi", pending = false),
            Said(2, "assistant", "", pending = true),
        )
        val live = Transcript.turns("English", pending, skipId = 2)
        assertEquals(2, live.size)
        assertEquals("Hi", live.last().content)
    }
}
