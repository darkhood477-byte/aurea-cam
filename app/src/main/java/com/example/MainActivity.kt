package com.example

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.camera.CameraScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

enum class PhysicalButtonAction {
    VOLUME_UP,
    VOLUME_DOWN,
    VOLUME_UP_LONG,
    VOLUME_DOWN_LONG
}

object PhysicalButtonRegistry {
    private val _events = MutableSharedFlow<PhysicalButtonAction>(extraBufferCapacity = 15)
    val events = _events.asSharedFlow()

    fun emit(action: PhysicalButtonAction) {
        _events.tryEmit(action)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Configure immersive mode: hide system bars and allow them to show transiently on swipe
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CameraScreen()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            event?.startTracking()
            PhysicalButtonRegistry.emit(PhysicalButtonAction.VOLUME_UP)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            event?.startTracking()
            PhysicalButtonRegistry.emit(PhysicalButtonAction.VOLUME_DOWN)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            PhysicalButtonRegistry.emit(PhysicalButtonAction.VOLUME_UP_LONG)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            PhysicalButtonRegistry.emit(PhysicalButtonAction.VOLUME_DOWN_LONG)
            return true
        }
        return super.onKeyLongPress(keyCode, event)
    }
}
