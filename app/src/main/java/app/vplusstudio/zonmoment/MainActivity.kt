package app.vplusstudio.zonmoment

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.vplusstudio.zonmoment.ui.HomeScreen
import app.vplusstudio.zonmoment.ui.MainViewModel
import app.vplusstudio.zonmoment.ui.SetupScreen
import app.vplusstudio.zonmoment.ui.ZonmomentTheme
import app.vplusstudio.zonmoment.work.DailyAdvice

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DailyAdvice.createChannel(this)
        setContent {
            ZonmomentTheme {
                val vm: MainViewModel = viewModel()
                val notificationPermission = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { /* the app works either way; only the morning notification needs it */ }

                Scaffold(Modifier.fillMaxSize()) { padding ->
                    val settings = vm.settings
                    if (vm.editing || settings == null) {
                        BackHandler(enabled = settings != null) { vm.cancelEditing() }
                        SetupScreen(
                            initial = settings,
                            onSaved = vm::saveSettings,
                            onCancel = if (settings != null) vm::cancelEditing else null,
                            modifier = Modifier.padding(padding),
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                        HomeScreen(
                            settings = settings,
                            state = vm.home,
                            onRefresh = vm::refresh,
                            onSettings = vm::startEditing,
                            modifier = Modifier.padding(padding),
                        )
                    }
                }
            }
        }
    }
}
