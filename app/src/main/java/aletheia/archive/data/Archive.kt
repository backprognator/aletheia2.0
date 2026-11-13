package aletheia.archive.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "conversations")
data class Conversation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "messages",
    indices = [Index("conversationId")],
)
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val role: String,
    val text: String,
    val pending: Boolean,
    val createdAt: Long,
)

@Entity(tableName = "passages")
data class Passage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: Long,
    val messageId: Long,
    val sourceTitle: String,
    val text: String,
    val copiedAt: Long,
)

data class ArchiveRow(
    val id: Long,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val replies: Int,
)

@Dao
interface ArchiveDao {
    @Query(
        """
        SELECT conversations.id, conversations.title, conversations.createdAt, conversations.updatedAt,
          (SELECT COUNT(*) FROM messages
            WHERE messages.conversationId = conversations.id
              AND messages.role = 'assistant'
              AND messages.pending = 0
              AND messages.text != '') AS replies
        FROM conversations
        ORDER BY conversations.updatedAt DESC
        """,
    )
    fun archive(): Flow<List<ArchiveRow>>

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY id ASC")
    fun messages(id: Long): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY id ASC")
    suspend fun messagesOnce(id: Long): List<Message>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun conversation(id: Long): Conversation?

    @Query("SELECT * FROM passages ORDER BY copiedAt DESC")
    fun passages(): Flow<List<Passage>>

    @Query("SELECT * FROM passages WHERE text = :text LIMIT 1")
    suspend fun passageByText(text: String): Passage?

    @Insert
    suspend fun insertConversation(conversation: Conversation): Long

    @Insert
    suspend fun insertMessage(message: Message): Long

    @Insert
    suspend fun insertPassage(passage: Passage): Long

    @Query("UPDATE messages SET text = :text, pending = :pending WHERE id = :id")
    suspend fun updateMessage(id: Long, text: String, pending: Boolean)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: Long, title: String, updatedAt: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :id")
    suspend fun deleteMessages(id: Long)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    @Query("DELETE FROM passages WHERE id = :id")
    suspend fun deletePassage(id: Long)
}

@Database(
    entities = [Conversation::class, Message::class, Passage::class],
    version = 1,
    exportSchema = false,
)
abstract class ArchiveDatabase : RoomDatabase() {
    abstract val dao: ArchiveDao

    companion object {
        fun create(context: Context): ArchiveDatabase {
            return Room.databaseBuilder(context, ArchiveDatabase::class.java, "aletheia-archive.db")
                .build()
        }
    }
}
