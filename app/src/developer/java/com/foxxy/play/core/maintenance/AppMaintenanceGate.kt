package com.foxxy.play.core.maintenance

import androidx.compose.runtime.Composable

@Composable
fun AppMaintenanceGate(content: @Composable () -> Unit) {
    content()
}
