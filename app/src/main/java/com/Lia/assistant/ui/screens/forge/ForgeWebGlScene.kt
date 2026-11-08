package com.Lia.assistant.ui.screens.forge

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.Lia.assistant.forge.ForgeUiState
import com.Lia.assistant.ui.theme.LocalNovaColors

class ForgeBridge(
    private val onReadyCallback: () -> Unit,
    private val onRevealFinishedCallback: () -> Unit,
    private val onFailedCallback: () -> Unit
) {
    @JavascriptInterface
    fun onReady() {
        onReadyCallback()
    }

    @JavascriptInterface
    fun onRevealFinished() {
        onRevealFinishedCallback()
    }

    @JavascriptInterface
    fun onWebGLFailed() {
        onFailedCallback()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ForgeWebGlScene(
    state: ForgeUiState,
    onReady: () -> Unit,
    onRevealFinished: () -> Unit,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current

    val bridge = remember {
        ForgeBridge(
            onReadyCallback = onReady,
            onRevealFinishedCallback = onRevealFinished,
            onFailedCallback = onFailed
        )
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            val assetLoader = WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                .build()

            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = false
                settings.allowFileAccess = false
                settings.allowContentAccess = false

                setBackgroundColor(android.graphics.Color.parseColor("#06050E"))

                addJavascriptInterface(bridge, "ForgeBridge")

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        return request?.url?.let { assetLoader.shouldInterceptRequest(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        val themeJson = """
                            {
                                "background": "#06050E",
                                "accent": "#FFB547",
                                "lagoon": "#3FE0D0",
                                "lotus": "#FF6FAE"
                            }
                        """.trimIndent()
                        view?.evaluateJavascript("window.Forge?.init('$themeJson')", null)
                    }
                }

                loadUrl("https://appassets.androidplatform.net/assets/forge/forge.html")
            }
        },
        update = { webView ->
            val stageIndex = state.stage.ordinal
            val stateJson = """{"progress":${state.progress},"stageIndex":$stageIndex}"""
            webView.evaluateJavascript("window.Forge?.update('$stateJson')", null)
        }
    )
}
