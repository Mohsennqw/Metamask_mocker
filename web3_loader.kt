import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class WebViewSetup(
    private val activity: Activity,
    private val context: Context,
    private val webView: WebView,
    private val walletAddress: String,
    private val editText: EditText,
    private val progressBar: ProgressBar,
    private val refreshLayout: SwipeRefreshLayout,
    private val chain: String
) {


    init {
        webView.webChromeClient = MyWebChromeClient(activity)
    }

    fun setupWebView(url: String) {
        // Add JavaScript interface
        webView.addJavascriptInterface(JavaScriptInterface(context, webView), "Android")

        // Enable hardware acceleration
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        // Enable cache
        webView.settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK

        // Enable JavaScript
        webView.settings.javaScriptEnabled = true

        // Enable file access
        webView.settings.allowFileAccess = true

        // Configure WebViewClient
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                view.loadUrl(request.url.toString())
                injectJavaScript()

                webView.addJavascriptInterface(JavaScriptInterface(context, webView), "Android")

                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                injectJavaScript()
                webView.addJavascriptInterface(JavaScriptInterface(context, webView), "Android")
                refreshLayout.isRefreshing = false
                if (url == "file:///android_asset/index.html" || url!!.startsWith("file")) {
                    editText.setText("")
                } else {
                    editText.setText(url ?: "")
                }
                progressBar.isVisible = false

            }


        }

        // Configure WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setLoadWithOverviewMode(true)
            setUseWideViewPort(true)
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            loadsImagesAutomatically = true
            blockNetworkLoads = false
            blockNetworkImage = false
            saveFormData = true
            savePassword = true
            useWideViewPort = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
        }

        // Load the URL
        if (url.startsWith("file")) {
            webView.loadUrl(url)
        } else {
            val modifiedUrl = fixUrl(url)
            webView.loadUrl(modifiedUrl)
        }
    }


    private fun fixUrl(url: String): String {
        var fixedUrl = url
        if (!fixedUrl.startsWith("http://") && !fixedUrl.startsWith("https://")) {
            if (fixedUrl.startsWith("www.")) {
                fixedUrl = "https://$fixedUrl"
            } else {
                fixedUrl = "https://$fixedUrl"
            }
        } else if (fixedUrl.startsWith("http://")) {
            fixedUrl = fixedUrl.replace("http://", "https://")
        }
        return fixedUrl
    }

    private fun injectJavaScript() {
        Eth_Mocker(webView,context).Mock(chain,walletAddress)
    }

    class MyWebChromeClient(private val activity: Activity) : WebChromeClient() {

        override fun onShowFileChooser(
            mWebView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            if (uploadMessage != null) {
                uploadMessage!!.onReceiveValue(null)
                uploadMessage = null
            }
            uploadMessage = filePathCallback
            val intent = fileChooserParams.createIntent()
            try {
                activity.startActivityForResult(intent, REQUEST_SELECT_FILE)
            } catch (e: ActivityNotFoundException) {
                uploadMessage = null
                Toast.makeText(activity, "Cannot Open File Chooser", Toast.LENGTH_LONG).show()
                return false
            }


            println(intent?.data)
            println(uploadMessage)
            println(mUploadMessage)
            return true
        }


        companion object {
            const val REQUEST_SELECT_FILE = 100
            const val FILECHOOSER_RESULTCODE = 1
            var uploadMessage: ValueCallback<Array<Uri>>? = null
            var mUploadMessage: ValueCallback<Uri?>? = null
        }
    }


}







