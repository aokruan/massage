package ru.aokruan.hmlkbi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import ru.aokruan.androidapp.BuildConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            App(
                baseUrl = BuildConfig.BASE_URL,
                enableLogging = BuildConfig.DEBUG
            )
        }
    }
}
