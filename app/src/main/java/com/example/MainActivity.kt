package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.RafeeqMainScreen
import com.example.ui.RafeeqViewModel
import com.example.ui.theme.RafeeqTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RafeeqViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RafeeqTheme {
                RafeeqMainScreen(viewModel = viewModel)
            }
        }
    }
}

