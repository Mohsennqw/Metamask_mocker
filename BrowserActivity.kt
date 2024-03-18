package com.bonegear.nfxwallet.dappbrowser

import WebViewSetup
import WebViewSetup.MyWebChromeClient.Companion.mUploadMessage
import WebViewSetup.MyWebChromeClient.Companion.uploadMessage
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bonegear.nfxwallet.MainActivity
import com.bonegear.nfxwallet.R
import com.bonegear.nfxwallet.databinding.ActivityBrowserBinding


class BrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBrowserBinding
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var editText: EditText
    private lateinit var sharedPreferences: SharedPreferences
    private  lateinit var ref : SwipeRefreshLayout

    private val REQUEST_SELECT_FILE = 100
    private val FILECHOOSER_RESULTCODE = 1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val customToolbar: Toolbar = findViewById(R.id.custom_toolbar)
        editText = customToolbar.findViewById(R.id.dapp_link)
        sharedPreferences = this.getSharedPreferences("signin", Context.MODE_PRIVATE)
        webView = findViewById(R.id.web_view)
        progressBar = findViewById(R.id.progress_bar)
        ref = binding.webviewRefresh
        ref.setDistanceToTriggerSync(300)
        ref.setSlingshotDistance(500)

        WebViewSetup(
            this,
            this,
            webView,
            sharedPreferences.getString("wallet_address", "").toString(),
            editText,
            progressBar,
            ref,
            "0x89"
        ).setupWebView("file:///android_asset/index.html")
        getDapp()
        refresh()






    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        println(requestCode)
        println(resultCode)
        println(intent?.data)
        println(uploadMessage)
        println(mUploadMessage)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null) return
                uploadMessage?.onReceiveValue(
                    WebChromeClient.FileChooserParams.parseResult(
                        resultCode,
                        intent
                    )
                )
                uploadMessage = null
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (mUploadMessage == null) return
            val result =
                if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
            mUploadMessage?.onReceiveValue(result)
            mUploadMessage = null
        } else Toast.makeText(this, "Failed to Upload Image", Toast.LENGTH_LONG).show()
    }


    private fun setupWebView(chain: String, url: String) {
        val walletAddress = sharedPreferences.getString("wallet_address", "").toString()
        if (!editText.text.toString().isNullOrBlank()) {
            val ref = binding.webviewRefresh
            progressBar.apply {
                isVisible = true
                setProgress(25)
            }
            WebViewSetup(this,this, webView, walletAddress, editText, progressBar, ref, chain).setupWebView(url)
        } else {
            Toast.makeText(this, "Fill the dapp link!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getDapp() {
        val customToolbar: Toolbar = findViewById(R.id.custom_toolbar)
        val searchButton: ImageButton = customToolbar.findViewById(R.id.search_dapp)
        val navigationButton: ImageButton = customToolbar.findViewById(R.id.navigation_button_back)

        navigationButton.setOnClickListener(){
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        searchButton.setOnClickListener {
            showChainSelectionDialog()
        }
    }

    private fun showChainSelectionDialog() {
        val dialogView = layoutInflater.inflate(R.layout.chain_selector, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.selector_Confirm).setOnClickListener {
            val radioGroup = dialogView.findViewById<RadioGroup>(R.id.radioGroup)
            val selectedChainId = radioGroup.checkedRadioButtonId
            dialog.setCancelable(false)
            when (selectedChainId) {
                R.id.radioButtonEth -> setupWebView("0x1", editText.text.toString())
                R.id.radioButtonPolygon -> setupWebView("0x89", editText.text.toString())
                R.id.radioButtonBsc -> setupWebView("0x38", editText.text.toString())
                else -> Toast.makeText(this, "Select a chain!", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun refresh() {
        val ref = binding.webviewRefresh
        ref.setOnRefreshListener {
            webView.clearCache(true)
            webView.reload()
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}
