package com.smsmurilo.telecom.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smsmurilo.telecom.mvi.HomeEffect
import com.smsmurilo.telecom.mvi.HomeViewModel
import org.koin.androidx.compose.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val viewModel: HomeViewModel = koinViewModel()
                val state by viewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(Unit) {
                    viewModel.effects.collect { effect ->
                        when (effect) {
                            is HomeEffect.Toast -> Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                            is HomeEffect.CopyToClipboard -> {
                                getSystemService(ClipboardManager::class.java)
                                    .setPrimaryClip(ClipData.newPlainText("Código de barras", effect.text))
                                Toast.makeText(this@MainActivity, "Código copiado", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                HomeScreen(state, viewModel::process)
            }
        }
    }
}
