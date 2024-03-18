import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import java.io.BufferedReader

@SuppressLint("SetJavaScriptEnabled")
class Typed_data_Sign(private val webView: WebView, private val context: Context) {

    private var onSignatureSetListener: ((String) -> Unit)? = null

    fun replacer(androidData: String, androidPrivateKey: String, onSignatureSet: (String) -> Unit) {
        this.onSignatureSetListener = onSignatureSet

        val read = readAsset(context, "bundled2.js")
        val modifiedRead = read
            .replace(" let Android_Data = {} ;", "let Android_Data = $androidData;")
            .replace(" let Android_PrivateKey = `` ;", " let Android_PrivateKey =`$androidPrivateKey`;")

        webView.evaluateJavascript(modifiedRead, null)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                val message = consoleMessage.message()

                if (message.contains("typed_signature")) {
                    val startIndex = message.indexOf("\"") + 1
                    val endIndex = message.lastIndexOf("\"")
                    val signatureValue = message.substring(startIndex, endIndex)
                    onSignatureSetListener?.invoke(signatureValue)
                }

                return true
            }
        }
    }

    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use(BufferedReader::readText)
    }



    companion object {
        var typed_signature = ""
    }
}
