package aletheia.archive

import aletheia.archive.data.ArchiveDatabase
import aletheia.archive.data.SettingsStore
import aletheia.wire.OpenRouterClient
import android.app.Application

class AletheiaApp : Application() {
    lateinit var database: ArchiveDatabase
        private set
    lateinit var settings: SettingsStore
        private set
    lateinit var openRouter: OpenRouterClient
        private set

    override fun onCreate() {
        super.onCreate()
        database = ArchiveDatabase.create(this)
        settings = SettingsStore(this)
        openRouter = OpenRouterClient()
    }
}
