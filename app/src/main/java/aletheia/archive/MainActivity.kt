package aletheia.archive

import aletheia.archive.ui.AletheiaTheme
import aletheia.archive.ui.ArchiveRoot
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as AletheiaApp
        setContent {
            AletheiaTheme {
                val model: ArchiveViewModel = viewModel(factory = ArchiveViewModel.factory(app))
                ArchiveRoot(model)
            }
        }
    }
}
