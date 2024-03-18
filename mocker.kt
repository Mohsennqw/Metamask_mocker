
import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebView
import java.io.BufferedReader

@SuppressLint("SetJavaScriptEnabled")
class Eth_Mocker(private val webView: WebView, private val context: Context) {


    fun Mock(chain: String, walletAddress: String) {
        val read = readAsset(context, "mocker.js")
            .replace("\$chain", chain)  // Escape $ to prevent string interpolation in Kotlin
            .replace("\$walletAddress", walletAddress)  // Escape $ to prevent string interpolation in Kotlin
        webView.evaluateJavascript(read, null)
    }


    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use(BufferedReader::readText)
    }




}
