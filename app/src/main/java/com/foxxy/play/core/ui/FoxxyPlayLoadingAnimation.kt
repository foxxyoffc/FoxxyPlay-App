package com.foxxy.play.core.ui

import android.graphics.Color as AndroidColor
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun FoxxyPlayLoadingAnimation(
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = true
                settings.setSupportZoom(false)

                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false

                setBackgroundColor(AndroidColor.TRANSPARENT)
                webViewClient = WebViewClient()

                loadUrl("file:///android_asset/foxxyplay_loading.svg")
            }
        },
        update = {}
    )
}
