package com.foxxy.play

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.foxxy.play.core.navigation.FoxxyNavigation
import com.foxxy.play.core.maintenance.AppMaintenanceGate
import com.foxxy.play.core.ui.theme.FoxxyPlayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            BuildConfig.FLAVOR == "public" &&
            Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                701
            )
        }

        setContent {
            FoxxyPlayTheme(dynamicColor = true) {
                Surface {
                    AppMaintenanceGate {
                        FoxxyNavigation()
                    }
                }
            }
        }
    }
}
