package wtf.cuteslavicboy.smolishapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
class MainActivity : Activity() {

    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        progress = findViewById(R.id.progress)
        webView = findViewById(R.id.web)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                route(request.url)

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (progress.progress >= 100) progress.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
            }

            override fun onShowFileChooser(
                view: WebView?,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams,
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                startActivityForResult(Intent.createChooser(intent, getString(R.string.file_chooser_title)), REQUEST_FILE)
                return true
            }
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(deepLinkFrom(intent) ?: HOME_URL)
        }
    }

    private fun route(url: Uri): Boolean {
        val scheme = url.scheme?.lowercase()
        if (scheme == "intent") return openExternally(Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME))
        if (scheme == "http" || scheme == "https") return false
        return openExternally(url)
    }

    private fun openExternally(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (e: Exception) {
        false
    }

    private fun openExternally(url: Uri): Boolean = openExternally(Intent(Intent.ACTION_VIEW, url))

    private fun deepLinkFrom(intent: Intent?): String? {
        val data = intent?.data ?: return null
        val scheme = data.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = data.host?.lowercase() ?: return null
        if (host != HOST && host != "www.$HOST" && host != "m.$HOST") return null
        return data.toString()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val url = deepLinkFrom(intent)
        if (url != null && ::webView.isInitialized) webView.loadUrl(url)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::webView.isInitialized) webView.saveState(outState)
    }

    override fun onPause() {
        super.onPause()
        if (::webView.isInitialized) webView.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onResume() {
        super.onResume()
        if (::webView.isInitialized) webView.onResume()
    }

    override fun onDestroy() {
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_FILE) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        val callback = filePathCallback
        filePathCallback = null
        callback ?: return
        val uris: Array<Uri>? = when {
            resultCode != RESULT_OK -> null
            data == null -> null
            data.clipData != null -> (0 until data.clipData!!.itemCount).map { data.clipData!!.getItemAt(it).uri }
                .toTypedArray()
            data.data != null -> arrayOf(data.data!!)
            else -> null
        }
        callback.onReceiveValue(uris)
    }

    companion object {
        private const val HOME_URL = "https://smolish.com/"
        private const val HOST = "smolish.com"
        private const val REQUEST_FILE = 0x51
    }
}