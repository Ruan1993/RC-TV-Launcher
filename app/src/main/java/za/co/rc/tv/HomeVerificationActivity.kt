package za.co.rc.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Neutral test surface: never starts a launcher or synthesises Home. */
class HomeVerificationActivity : ComponentActivity() {
    override fun onDestroy() {
        if (isFinishing) runCatching { za.co.rc.tv.core.SetupSession(this).cancelHomeTest() }
        super.onDestroy()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Surface(Modifier.fillMaxSize()) {
            Column(Modifier.padding(32.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("Press Home on your physical remote", style = MaterialTheme.typography.headlineMedium)
                Text("If RC TV opens, continue setup there. If another launcher opens, reopen RC TV from Apps. Back cancels the test.")
                Button(onClick = { finish() }) { Text("Cancel test") }
            }
        } } }
    }
}
