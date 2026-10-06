package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.notification.NotificationHelper
import com.example.ui.navigation.MainFinanceApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FinanceViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createNotificationChannel(this)

        setContent {
            MyApplicationTheme {
                val financeViewModel: FinanceViewModel = viewModel()
                MainFinanceApp(viewModel = financeViewModel)
            }
        }
    }
}
