@file:Suppress("LiftReturnOrAssignment")

package me.uport.sdk.jsonrpc

import com.squareup.moshi.Json
import me.uport.sdk.core.urlPost
import org.kethereum.extensions.hexToBigInteger
import org.kethereum.functions.encodeRLP
import org.kethereum.model.SignatureData
import org.kethereum.model.Transaction
import org.walleth.khex.toHexString
import java.math.BigInteger


class JsonRPC(private val rpcUrl: String) {

//=============================
// eth_gasPrice
//=============================

    /**
     * calls back with the gas price in Wei or an error if one occurred
     */
    fun getGasPrice(callback: (err: Exception?, nonce: BigInteger) -> Unit) {
        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_gasPrice",
                params = emptyList()
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, BigInteger.ZERO)
            }
            val parsedResponse = JsonRpcBaseResponse.fromJson(rawResult)
            val priceInWei = parsedResponse.result.toString().hexToBigInteger()
            return@urlPost callback(null, priceInWei)
        }
    }


//=============================
//eth_getTransactionCount
//=============================

    /**
     * Calls back with the number of transactions made from the given address.
     * The number is usable as `nonce` (since nonce is zero indexed)
     */
    fun getTransactionCount(address: String, callback: (err: Exception?, count: BigInteger) -> Unit) {
        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_getTransactionCount",
                params = listOf(address, "latest")
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, BigInteger.ZERO)
            }
            val parsedResponse = JsonRpcBaseResponse.fromJson(rawResult)
            val count = parsedResponse.result.toString().hexToBigInteger()
            return@urlPost callback(null, count)
        }
    }


//=============================
//eth_getBalance
//=============================

    /**
     * Calls back with the number of transactions made from the given address.
     * The number is usable as `nonce` (since nonce is zero indexed)
     */
    fun getAccountBalance(address: String, callback: (err: Exception?, count: BigInteger) -> Unit) {
        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_getBalance",
                params = listOf(address, "latest")
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, BigInteger.ZERO)
            }
            val parsedResponse = JsonRpcBaseResponse.fromJson(rawResult)
            val balanceInWei = parsedResponse.result.toString().hexToBigInteger()
            return@urlPost callback(null, balanceInWei)
        }
    }


//=============================
// eth_getTransactionReceipt
//=============================

    class JsonRpcReceiptResponse(override val result: TransactionReceipt?) : JsonRpcBaseResponse() {

        companion object {
            private val adapter by lazy {
                moshi.adapter<JsonRpcReceiptResponse>(JsonRpcReceiptResponse::class.java)
            }

            fun fromJson(json: String) = adapter.fromJson(json)
        }
    }

    data class TransactionReceipt(

            @Json(name = "transactionHash")
            val transactionHash: String? = "",

            @Json(name = "transactionIndex")
            val transactionIndex: String? = "",

            @Json(name = "blockNumber")
            val blockNumber: String? = "",

            @Json(name = "blockHash")
            val blockHash: String? = "",

            @Json(name = "cumulativeGasUsed")
            val cumulativeGasUsed: String? = "",

            @Json(name = "contractAddress")
            val contractAddress: String? = null,

            //TODO: better encapsulate logs
            @Suppress("ArrayInDataClass")
            @Json(name = "logs")
            val logs: Array<Any?>? = null,

            @Json(name = "logsBloom")
            val logsBloom: String? = "",

            @Json(name = "status")
            val status: String? = "0x0"
    )

    fun getTransactionReceipt(txHash: String, callback: (err: Exception?, receipt: TransactionReceipt) -> Unit) {
        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_getTransactionReceipt",
                params = arrayListOf(txHash)
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, TransactionReceipt())
            }
            val parsedResponse = JsonRpcReceiptResponse.fromJson(rawResult)

            if (parsedResponse?.result != null) {
                return@urlPost callback(null, parsedResponse.result)
            } else {
                return@urlPost callback(TransactionNotFound(txHash), TransactionReceipt(transactionHash = txHash))
            }
        }
    }


//=============================
// eth_getTransactionByHash
//=============================

    class JsonRpcTxByHashResponse(override val result: TransactionInformation) : JsonRpcBaseResponse() {

        companion object {
            private val adapter by lazy {
                moshi.adapter<JsonRpcTxByHashResponse>(JsonRpcTxByHashResponse::class.java)
            }

            fun fromJson(json: String) = adapter.fromJson(json)
        }
    }

    fun getTransactionByHash(txHash: String, callback: (err: Exception?, receipt: TransactionInformation) -> Unit) {
        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_getTransactionByHash",
                params = arrayListOf(txHash)
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, TransactionInformation())
            }

            val parsedResponse = JsonRpcTxByHashResponse.fromJson(rawResult)

            if (parsedResponse?.result != null) {
                return@urlPost callback(null, parsedResponse.result)
            } else {
                return@urlPost callback(TransactionNotFound(txHash), TransactionInformation(txHash = txHash))
            }
        }
    }

    data class TransactionInformation(
            @Json(name = "hash")
            val txHash: String? = null,

            @Json(name = "nonce")
            val nonce: String? = null,

            @Json(name = "blockHash")
            val blockHash: String? = null,

            @Json(name = "blockNumber")
            val blockNumber: String? = null,

            @Json(name = "transactionIndex")
            val transactionIndex: String? = null,

            @Json(name = "from")
            val from: String = "",

            @Json(name = "to")
            val to: String = "",

            @Json(name = "value")
            val value: String = "",

            @Json(name = "gas")
            val gas: String = "",

            @Json(name = "gasPrice")
            val gasPrice: String = "",

            @Json(name = "input")
            val input: String = ""
    )


//=============================
//eth_sendRawTransaction
//=============================

    fun sendRawTransaction(
            transaction: Transaction,
            signature: SignatureData,
            callback: (err: Exception?, txHash: String) -> Unit) {

        return sendRawTransaction(transaction.encodeRLP(signature).toHexString(), callback)
    }

    fun sendRawTransaction(
            signedTx: String,
            callback: (err: Exception?, txHash: String) -> Unit) {

        val payloadRequest = JsonRpcBaseRequest(
                method = "eth_sendRawTransaction",
                params = listOf(signedTx)
        ).toJson()

        urlPost(rpcUrl, payloadRequest) { err, rawResult ->
            if (err != null) {
                return@urlPost callback(err, "")
            }

            val parsedResponse = JsonRpcBaseResponse.fromJson(rawResult)
            val txHash = parsedResponse.result.toString()
            return@urlPost callback(null, txHash)
        }
    }

}

class TransactionNotFound(txHash: String) : RuntimeException("The transaction with hash=$txHash has not been mined yet")



