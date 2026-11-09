package de.martinkloeckner.rezeptbuch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import de.martinkloeckner.rezeptbuch.ui.AppRoot
import de.martinkloeckner.rezeptbuch.ui.RezeptbuchTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels {
        MainViewModel.Factory((application as RezeptbuchApp).container.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RezeptbuchTheme { AppRoot(vm) }
        }
    }
}
