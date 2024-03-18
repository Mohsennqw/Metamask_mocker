import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.bonegear.nfxwallet.R
import com.google.gson.GsonBuilder
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.Sign
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt
import org.web3j.protocol.core.methods.response.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigDecimal
import java.math.BigInteger
import java.util.concurrent.CompletableFuture


class JavaScriptInterface(private val context: Context, private val webView: WebView) {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(
            "signin",
            Context.MODE_PRIVATE
        )
    }

    @Volatile
    private var signature2 = ""

    private var balance: String = ""
    private var hash: String = ""
    private var block_number: String = ""


    @JavascriptInterface
    fun signer(message: String) {
        signature2 = ""
        val builder = AlertDialog.Builder(context)
        val normal_message = if (message.startsWith("0x")) {
            hexToNormal(message)
        } else {
            message
        }
        builder.setTitle("Confirmation")
            .setMessage(normal_message)
            .setPositiveButton("Yes") { dialog, which ->
                val privateKeyHex = sharedPreferences.getString("private_key", "") ?: ""

                val messageBytes = if (message.startsWith("0x")) {
                    // If the message is already in hexadecimal format, convert it to bytes
                    Numeric.hexStringToByteArray(message)
                } else {
                    // If the message is normal text, convert it to hexadecimal bytes
                    message.toByteArray()
                }

                // Convert private key to ECKeyPair
                val credentials = Credentials.create(privateKeyHex)

                // Sign the message
                val signatureData = Sign.signPrefixedMessage(messageBytes, credentials.ecKeyPair)

                // Concatenate r, s, v to create the signature string
                val r = signatureData.r
                val s = signatureData.s
                val v = signatureData.v

                val value = ByteArray(65)
                System.arraycopy(r, 0, value, 0, 32)
                System.arraycopy(s, 0, value, 32, 32)
                System.arraycopy(v, 0, value, 64, 1)

                signature2 = Numeric.toHexString(value)
                Log.d("Signature", "Signature: $signature2")

                // Once the signature is obtained, show the dialog
                geter()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
                // Call the callback with null to indicate cancellation
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    @SuppressLint("SuspiciousIndentation")
    @JavascriptInterface
    fun signer_typed_data(message1: String, message2: String) {
        signature2 = ""
        val builder = AlertDialog.Builder(context)

        builder.setTitle("Confirmation")
            .setMessage(message1)
            .setPositiveButton("Yes") { dialog, _ ->
                val privateKeyHex = sharedPreferences.getString("private_key", "") ?: ""

                Handler(Looper.getMainLooper()).post {
                    val sign0 = Typed_data_Sign(webView, context)
                    sign0.replacer(message1, privateKeyHex) { signature ->
                        signature2 = signature
                    }
                }


            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                // Call the callback with null to indicate cancellation
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }


    @Synchronized
    @JavascriptInterface
    fun geter(): String {
        return signature2
        signature2 = ""

    }


    @JavascriptInterface
    fun confirmer(): Boolean {
        return signature2!!.isNotEmpty()
    }

    @JavascriptInterface
    fun balance(chain: String): String {
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"
        val walletAddress = sharedPreferences.getString("wallet_address", "") ?: ""

        val web3j = when (chain) {
            "0x1" -> Web3j.build(HttpService(ethNodeUrl))
            "0x89" -> Web3j.build(HttpService(maticNodeUrl))
            "0x38" -> Web3j.build(HttpService(bscNodeUrl))
            else -> null
        }

        val ethBalance = web3j?.ethGetBalance(
            walletAddress,
            DefaultBlockParameterName.LATEST
        )?.send()?.balance
        balance = Numeric.encodeQuantity(ethBalance.toString().toBigInteger())
        return balance
    }

    @JavascriptInterface
    fun sendTransactions(
        from: String,
        to: String,
        value: String,
        chains: String,
        data: String,
        gasPrice: String,
        gasLimit: String
    ) {

        Log.e("data", "$data")
        Log.e("to", "$to")
        Log.e("value", "$value")
        Log.e("chains", "$chains")
        Log.e("gasp", "$gasPrice")
        Log.e("gasl", "$gasLimit")


        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"
        val private_key = sharedPreferences.getString("private_key", "") ?: ""
        // Initialize hash variable to store transaction hash
        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }


        val web3j = Web3j.build(HttpService(nodeUrl))

        // Create credentials from private key
        val credentials = Credentials.create(private_key)
        val ethBalance =
            web3j?.ethGetBalance(from, DefaultBlockParameterName.LATEST)?.send()?.balance

        val valueInWei =
            if (value.isNullOrEmpty() || value == "undefined" || value == "0x0" || value == "0") {
                BigInteger.ZERO
            } else {
                parseValue(value)
            }
        val nonceResponse =
            web3j?.ethGetTransactionCount(credentials.address, DefaultBlockParameterName.LATEST)
                ?.send()
        val nonce = nonceResponse?.transactionCount ?: BigInteger.ZERO

        val gasPriceBigInt: BigInteger = if (gasPrice.isNullOrEmpty() || gasPrice == "undefined") {
            // If gasPrice is empty, calculate it using ethGasPrice
            val limit = web3j.ethGasPrice().send().gasPrice.toDouble() * 1.2
            limit.toBigDecimal().toBigInteger()
        } else {
            // Otherwise, parse the provided gasPrice
            parseHexString(gasPrice)
        }
        val gasLimitBigInt: BigInteger = if (gasLimit.isNullOrEmpty() || gasLimit == "undefined") {
            try {
                val rawData = data.removePrefix("0x")
                val byteLength =
                    rawData.length   // Each byte is represented by 2 hexadecimal characters
                val baseGas = 30000.toBigInteger() // Base gas for simple transactions
                val gasPerByte =
                    170.toBigDecimal().toBigInteger()// Additional gas per byte of transaction data
                val gasLimit = baseGas + (gasPerByte * byteLength.toBigInteger())
                println("Gas Estimate2: Gas Used: ${gasLimit}")
                gasLimit
            } catch (e: Exception) {
                showToast("Failed to estimate gas")
                return
            }
        } else {
            try {
                parseHexString(gasLimit)
            } catch (e: NumberFormatException) {
                Log.e("error", e.toString())
                showToast("Invalid gas limit format")
                return
            }
        }
        // Calculate gas cost
        val gasCost = gasPriceBigInt * gasLimitBigInt

        val alret_title = if (valueInWei == BigInteger.ZERO) {
            "Contract Apporove"
        } else {
            "Contract Call Transaction"
        }
        val eth_value = weiToEther(valueInWei)
        val eth_cost = weiToEther(gasCost)


        val dialogView = LayoutInflater.from(context).inflate(R.layout.eth_sender_browser, null)

// Initialize views
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message)
        val confirmButton = dialogView.findViewById<Button>(R.id.button_confirm)
        val cancelButton = dialogView.findViewById<Button>(R.id.button_cancel)

// Set title and message
        titleTextView.text = alret_title
        val builder = SpannableStringBuilder()
        builder.append("This website try to send a transaction from your wallet...\n")
        builder.append("Here is the Transaction Details:\n\n")

// Value
        val valueStart = builder.length
        builder.append("Value: $eth_value\n\n")
        val valueEnd = builder.length
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.txt_gray)),
            valueStart,
            valueEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// ChainId
        val chainIdStart = builder.length
        builder.append("ChainId: $chainId\n\n")
        val chainIdEnd = builder.length
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.txt_gray)),
            chainIdStart,
            chainIdEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Transaction Fee
        val feeStart = builder.length
        builder.append("Transaction Fee: $eth_cost\n\n")
        val feeEnd = builder.length
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.txt_gray)),
            feeStart,
            feeEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Contract
        val contractStart = builder.length
        builder.append("Contract: $to\n\n")
        val contractEnd = builder.length
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.txt_gray)),
            contractStart,
            contractEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Type
        val typeStart = builder.length
        builder.append("Type: $alret_title")
        val typeEnd = builder.length
        builder.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.txt_gray)),
            typeStart,
            typeEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

// Set the styled text to the messageTextView
        messageTextView.text = builder


// Create and show the dialog
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val layoutParams = WindowManager.LayoutParams()
        layoutParams.copyFrom(alertDialog.window?.attributes)
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        alertDialog.window?.attributes = layoutParams

        confirmButton.setOnClickListener {
            try {
                // Check account balance before sending funds
                if (valueInWei + gasCost > ethBalance) {
                    // Handle insufficient balance
                    showToast("Insufficient balance")

                }

                val rawTransaction = if (valueInWei == BigInteger.ZERO) {
                    // Transaction without value (0 ETH), contract deployment
                    RawTransaction.createTransaction(
                        nonce,
                        gasPriceBigInt,
                        gasLimitBigInt.multiply(BigInteger.valueOf(2)),
                        to, // Use the recipient address directly
                        valueInWei, // Use the calculated value in Wei
                        data // Contract call data
                    )
                } else {
                    // Transaction with value (non-zero ETH), contract interaction
                    RawTransaction.createTransaction(
                        nonce,
                        gasPriceBigInt,
                        gasLimitBigInt,
                        to, // Use the recipient address directly
                        valueInWei, // Use the calculated value in Wei
                        data // Contract call data
                    )
                }

// Sign the raw transaction
                val signedTransaction =
                    TransactionEncoder.signMessage(rawTransaction, chainId.toLong(), credentials)
                val hexValue = Numeric.toHexString(signedTransaction)

// Send the signed transaction
                val receipt = web3j.ethSendRawTransaction(hexValue).send()

                if (receipt.hasError()) {
                    // Handle error
                    val error = receipt.error.message
                    alertDialog.dismiss()
                } else {
                    // Transaction successful
                    val transactionHash = receipt.transactionHash
                    println(transactionHash)
                    hash = transactionHash
                    alertDialog.dismiss()
                }


            } catch (e: Exception) {
                // Handle exceptions
                showToast(e.toString())
                Log.e("tage", e.toString())
                alertDialog.dismiss()
            }
        }
        cancelButton.setOnClickListener {
            alertDialog.dismiss()
            hash = ""
        }


        alertDialog.show()


    }

    @JavascriptInterface
    fun eth_transaction(): String {
        // Get the current value of hash
        val currentHash = hash
        // Clear the hash for next transaction
        hash = ""
        // Return the current value of hash
        return currentHash
    }

    @JavascriptInterface
    fun block_number(chains: String): String {
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"
        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }

        val web3j = Web3j.build(HttpService(nodeUrl))
        val latestBlock =
            web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().block
        val latestBlockNumber = latestBlock.number
        val hexed_block = Numeric.encodeQuantity(latestBlockNumber)
        return hexed_block
    }

    @JavascriptInterface
    fun getBlockByNumber(chains: String): String {
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"
        val private_key = sharedPreferences.getString("private_key", "") ?: ""

        // Initialize hash variable to store transaction hash
        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }

        val web3j = Web3j.build(HttpService(nodeUrl))

        val latestBlock =
            web3j.ethGetBlockByNumber(DefaultBlockParameterName.LATEST, false).send().block
        val latestBlockNumber = latestBlock.number

        val ethBlockResponse =
            web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(latestBlockNumber), true).send()
        val block = ethBlockResponse.block

        if (block == null) {
            // Handle null block
            // Return an empty map or appropriate error message
            return ""
        }

        // Create map for the response
        val response = mutableMapOf<String, Any>()

        // Populate response with block details

        response["baseFeePerGas"] = "0x0"
        response["difficulty"] = block.difficulty?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["extraData"] = block.extraData ?: ""
        response["gasLimit"] = block.gasLimit?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["gasUsed"] = block.gasUsed?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["hash"] = block.hash ?: ""
        response["logsBloom"] = block.logsBloom ?: ""
        response["miner"] = block.miner ?: ""
        response["mixHash"] = block.mixHash ?: ""
        response["nonce"] = block.nonce?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["number"] = block.number?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["parentHash"] = block.parentHash ?: ""
        response["receiptsRoot"] = block.receiptsRoot ?: ""
        response["sha3Uncles"] = block.sha3Uncles ?: ""
        response["size"] = block.size?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["stateRoot"] = block.stateRoot ?: ""
        response["timestamp"] = block.timestamp?.let { Numeric.encodeQuantity(it) } ?: "0x0"
        response["totalDifficulty"] =
            block.totalDifficulty?.let { Numeric.encodeQuantity(it) } ?: "0x0"

        // Add transaction hashes to the list
        val transactions: List<EthBlock.TransactionObject> =
            ethBlockResponse.result.transactions as List<EthBlock.TransactionObject>
        val transactionHashes = transactions.map { it.hash }
        response["transactions"] = transactionHashes
        response["transactionsRoot"] = block.transactionsRoot ?: ""
        response["uncles"] = block.uncles?.map { it.toString() } ?: emptyList<String>()

        val gson = GsonBuilder().enableComplexMapKeySerialization().create()

        // Convert response map to JSON with pretty printing
        val toJson = gson.toJson(response)


        return toJson
    }

    @JavascriptInterface
    fun getTransactionCount(chainId: String): String {
        val address = sharedPreferences.getString("wallet_address", "") ?: ""
        println("chain" + chainId)
        println("address" + address)
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"

        var nodeUrl = ""
        when (chainId) {
            "1", "0x1" -> nodeUrl = ethNodeUrl
            "137", "0x89" -> nodeUrl = maticNodeUrl
            "56", "0x38" -> nodeUrl = bscNodeUrl
            else -> nodeUrl = ethNodeUrl
        }
        val web3j = Web3j.build(HttpService(nodeUrl))
        val nonceResponse =
            web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST)?.send()
        val nonce = nonceResponse?.transactionCount
        println(nonce)
        return if (nonce.toString().isNullOrEmpty()) {
            Numeric.encodeQuantity(BigInteger.ZERO)
        } else {
            Numeric.encodeQuantity(nonce)
        }
    }

    @JavascriptInterface
    fun eth_estimateGas(
        from: String,
        to: String,
        value: String,
        chains: String,
        data: String,
        gasPrice: String,
        gasLimit: String
    ): String {

        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"

        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }


        val web3j = Web3j.build(HttpService(nodeUrl))


        val gasPriceBigInt: BigInteger = if (gasPrice.isNullOrEmpty() || gasPrice == "undefined") {
            // If gasPrice is empty, calculate it using ethGasPrice
            val limit = web3j.ethGasPrice().send().gasPrice.toDouble() * 1.2
            limit.toBigDecimal().toBigInteger()
        } else {
            // Otherwise, parse the provided gasPrice
            parseHexString(gasPrice)
        }
        val gasLimitBigInt: BigInteger = if (gasLimit.isNullOrEmpty() || gasLimit == "undefined") {
            try {
                val rawData = data.removePrefix("0x")
                val byteLength =
                    rawData.length   // Each byte is represented by 2 hexadecimal characters
                val baseGas = 30000.toBigInteger() // Base gas for simple transactions
                val gasPerByte =
                    170.toBigDecimal().toBigInteger()// Additional gas per byte of transaction data
                val gasLimit = baseGas + (gasPerByte * byteLength.toBigInteger())
                println("Gas Estimate2: Gas Used: ${gasLimit}")
                gasLimit
            } catch (e: Exception) {
                showToast("Failed to estimate gas")
                return ""
            }
        } else {
            try {
                parseHexString(gasLimit)
            } catch (e: NumberFormatException) {
                showToast("Invalid gas limit format")
                return ""
            }
        }
        // Calculate gas cost
        val gasCost = gasPriceBigInt * gasLimitBigInt


        return Numeric.encodeQuantity(gasLimitBigInt)

    }

    @JavascriptInterface
    fun getTransactionByHash(txHash: String, chains: String): String {
        // Ethereum mainnet node URL
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"

        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }


        val web3j = Web3j.build(HttpService(nodeUrl))


        // Fetch the transaction by its hash
        val transactionResponse: Transaction =
            web3j.ethGetTransactionByHash(txHash).send().transaction.get()

        // Create a map to store the transaction details
        val transactionDetails = mutableMapOf<String, Any?>()

        // Populate the map with transaction details
        transactionDetails["blockHash"] = transactionResponse.blockHash
        transactionDetails["blockNumber"] = transactionResponse.blockNumberRaw
        transactionDetails["from"] = transactionResponse.from
        transactionDetails["gas"] = Numeric.encodeQuantity(transactionResponse.gas)
        transactionDetails["gasPrice"] = Numeric.encodeQuantity(transactionResponse.gasPrice)
        transactionDetails["hash"] = transactionResponse.hash
        transactionDetails["input"] = transactionResponse.input
        transactionDetails["nonce"] = Numeric.encodeQuantity(transactionResponse.nonce)
        transactionDetails["to"] = transactionResponse.to
        transactionDetails["transactionIndex"] =
            Numeric.encodeQuantity(transactionResponse.transactionIndex)
        transactionDetails["value"] = Numeric.encodeQuantity(transactionResponse.value)
        transactionDetails["type"] = "0x0"
        transactionDetails["chainId"] = Numeric.encodeQuantity(chainId.toBigInteger())
        transactionDetails["v"] = if (transactionResponse.v.toString().startsWith("0x")) {
            transactionResponse.v
        } else {
            Numeric.encodeQuantity(transactionResponse.v.toBigInteger())
        }

        transactionDetails["r"] = transactionResponse.r
        transactionDetails["s"] = transactionResponse.s

        // Convert the map to JSON string

        val gson = GsonBuilder().enableComplexMapKeySerialization().create()

        // Convert response map to JSON with pretty printing
        val toJson = gson.toJson(transactionDetails)

        return toJson
    }

    @JavascriptInterface
    fun eth_getTransactionReceipt(txHash: String, chains: String): String {
        var toJson = ""
        // Ethereum mainnet node URL
        val maticNodeUrl = "https://polygon-rpc.com/"
        val bscNodeUrl = "https://bsc-dataseed4.defibit.io/"
        val ethNodeUrl = "https://eth.meowrpc.com"

        var nodeUrl = ""
        var chainId = ""

        when (chains) {
            "1", "0x1" -> {
                nodeUrl = ethNodeUrl
                chainId = "1"
            }

            "137", "0x89" -> {
                nodeUrl = maticNodeUrl
                chainId = "137"
            }

            "56", "0x38" -> {
                nodeUrl = bscNodeUrl
                chainId = "56"
            }

            else -> {
                // Default to Ethereum mainnet
                nodeUrl = ethNodeUrl
                chainId = "1"
            }
        }


        val web3j = Web3j.build(HttpService(nodeUrl))


        // Fetch the transaction by its hash
        val transactionReceiptFuture: CompletableFuture<EthGetTransactionReceipt> =
            web3j.ethGetTransactionReceipt(txHash).sendAsync()

// Wait for the request to complete and get the transaction receipt
        val transactionReceipt: EthGetTransactionReceipt = transactionReceiptFuture.get()

        if (transactionReceipt.result != null) {


            // Create a map to store the transaction details
            val transactionDetails = mutableMapOf<String, Any?>()

            // Populate the map with transaction details
            transactionDetails["blockHash"] = transactionReceipt.result.blockHash
            transactionDetails["blockNumber"] =
                Numeric.encodeQuantity(transactionReceipt.result.blockNumber)
            transactionDetails["contractAddress"] = transactionReceipt.result.contractAddress
            transactionDetails["cumulativeGasUsed"] =
                Numeric.encodeQuantity(transactionReceipt.result.cumulativeGasUsed)
            transactionDetails["effectiveGasPrice"] = transactionReceipt.result.effectiveGasPrice
            transactionDetails["from"] = transactionReceipt.result.from
            transactionDetails["gasUsed"] =
                Numeric.encodeQuantity(transactionReceipt.result.gasUsed)
            transactionDetails["logs"] = transactionReceipt.result.logs
            transactionDetails["logsBloom"] = transactionReceipt.result.logsBloom
            transactionDetails["status"] = transactionReceipt.result.status
            transactionDetails["to"] = transactionReceipt.result.to
            transactionDetails["transactionHash"] = transactionReceipt.result.transactionHash
            transactionDetails["transactionIndex"] =
                Numeric.encodeQuantity(transactionReceipt.result.transactionIndex)
            transactionDetails["type"] = transactionReceipt.result.type


            val gson = GsonBuilder().enableComplexMapKeySerialization().create()

            // Convert response map to JSON with pretty printing
            toJson = gson.toJson(transactionDetails)


        } else {

            return "null"
        }

        return toJson
    }

    fun weiToEther(weiAmount: BigInteger): String {
        val etherAmount = Convert.fromWei(weiAmount.toString(), Convert.Unit.ETHER)
        return etherAmount.toPlainString()
    }

    fun parseHexString(hexString: String): BigInteger {
        // Remove '0x' prefix if present
        val cleanHexString = if (hexString.startsWith("0x")) hexString.substring(2) else hexString
        return BigInteger(cleanHexString, 16)
    }

    private fun parseValue(value: String): BigInteger {
        return try {
            if (value.startsWith("0x")) {
                // Value is in hex format, remove "0x" prefix and parse
                BigInteger(value.substring(2), 16)
            } else {
                // Try to parse the value directly
                BigDecimal(value).toBigInteger()
            }
        } catch (e: NumberFormatException) {
            // If parsing fails, try to parse as a floating point number
            val floatValue = value.toDouble()
            val bigDecimalValue = BigDecimal(floatValue)
            bigDecimalValue.toBigInteger()
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    fun hexToNormal(hex: String): String {
        // Remove the "0x" prefix if present
        val cleanHex = if (hex.startsWith("0x")) hex.substring(2) else hex

        // Convert the hexadecimal string to a byte array
        val byteArray = ByteArray(cleanHex.length / 2)
        for (i in cleanHex.indices step 2) {
            val byteValue = cleanHex.substring(i, i + 2).toInt(16)
            byteArray[i / 2] = byteValue.toByte()
        }

        // Create a string from the byte array
        return String(byteArray)
    }

}


fun main() {
    val web3j =
        Web3j.build(HttpService("https://polygon-rpc.com/")) // Replace with your Ethereum client URL

// Transaction hash for which you want to get the receipt
    val transactionHash = "0x99e30d58f799d0b051273443e11fd2fdae76fb319c2434370535e4cad28f4b4b"

// Send asynchronous request to get the transaction receipt
    val transactionReceiptFuture: CompletableFuture<EthGetTransactionReceipt> =
        web3j.ethGetTransactionReceipt(transactionHash).sendAsync()

// Wait for the request to complete and get the transaction receipt
    val transactionReceipt: EthGetTransactionReceipt = transactionReceiptFuture.get()

    if (transactionReceipt.result != null) {


        // Create a map to store the transaction details
        val transactionDetails = mutableMapOf<String, Any?>()

        // Populate the map with transaction details
        transactionDetails["blockHash"] = transactionReceipt.result.blockHash
        transactionDetails["blockNumber"] =
            Numeric.encodeQuantity(transactionReceipt.result.blockNumber)
        transactionDetails["contractAddress"] = transactionReceipt.result.contractAddress
        transactionDetails["cumulativeGasUsed"] =
            Numeric.encodeQuantity(transactionReceipt.result.cumulativeGasUsed)
        transactionDetails["effectiveGasPrice"] = transactionReceipt.result.effectiveGasPrice
        transactionDetails["from"] = transactionReceipt.result.from
        transactionDetails["gasUsed"] = Numeric.encodeQuantity(transactionReceipt.result.gasUsed)
        transactionDetails["logs"] = transactionReceipt.result.logs
        transactionDetails["logsBloom"] = transactionReceipt.result.logsBloom
        transactionDetails["status"] = transactionReceipt.result.status
        transactionDetails["to"] = transactionReceipt.result.to
        transactionDetails["transactionHash"] = transactionReceipt.result.transactionHash
        transactionDetails["transactionIndex"] =
            Numeric.encodeQuantity(transactionReceipt.result.transactionIndex)
        transactionDetails["type"] = transactionReceipt.result.type


        val gson = GsonBuilder().enableComplexMapKeySerialization().create()

        // Convert response map to JSON with pretty printing
        val toJson = gson.toJson(transactionDetails)

        println(toJson)


    } else {

        println("Transaction receipt is not available yet.")
    }
}