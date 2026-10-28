package com.risefit.app

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.risefit.app.ui.RiseFitApp
import com.risefit.app.ui.theme.RiseFitTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels()
    private val plus: PlusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        setContent {
            val settings by vm.settings.collectAsState()
            RiseFitTheme(settings.accentIndex) {
                RiseFitApp(vm, plus)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // the journal locks again whenever the app leaves the screen
        plus.lockJournal()
    }
}
