package com.rulecough.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rulecough.app.ui.RecordScreen
import com.rulecough.app.ui.ResultScreen
import com.rulecough.app.ui.SettingsScreen
import com.rulecough.app.ui.theme.RULeCoughTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RULeCoughTheme {
                MainApp()
            }
        }
    }
}

private enum class Tab { Record, Settings }

@Composable
private fun MainApp(vm: MainViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.Record) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // microphone permission
    var hasMic by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val micLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMic = granted
        if (granted) vm.startRecording()
    }

    // audio file picker
    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.onFilePicked(it) } }

    val state = vm.uiState

    Scaffold(
        bottomBar = {
            // Hide the nav bar while showing a full-screen result.
            if (state !is UiState.Success) {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == Tab.Record,
                        onClick = { tab = Tab.Record },
                        icon = { Icon(Icons.Filled.Mic, contentDescription = null) },
                        label = { Text("Record") }
                    )
                    NavigationBarItem(
                        selected = tab == Tab.Settings,
                        onClick = { tab = Tab.Settings },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state is UiState.Success -> ResultScreen(
                    result = state.result,
                    onAgain = { vm.reset(); tab = Tab.Record }
                )
                tab == Tab.Settings -> SettingsScreen(vm)
                else -> RecordScreen(
                    vm = vm,
                    hasMic = hasMic,
                    onRequestMic = {
                        if (hasMic) vm.startRecording()
                        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onPickFile = { filePicker.launch("audio/*") }
                )
            }
        }
    }
}
