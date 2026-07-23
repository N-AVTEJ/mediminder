package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.components.MainAppContainer
import com.example.ui.theme.MedicineReminderTheme
import com.example.ui.viewmodel.MedicineViewModel

class MainActivity : ComponentActivity() {

    private val medicineViewModel: MedicineViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicineReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppContainer(viewModel = medicineViewModel)
                }
            }
        }
    }
}
