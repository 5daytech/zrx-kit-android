package com.blocksdecoded.zrxkit.sign

import com.blocksdecoded.zrxkit.InvalidSignatureException
import com.blocksdecoded.zrxkit.UnsupportedSignatureType
import com.blocksdecoded.zrxkit.model.ESignatureType
import com.blocksdecoded.zrxkit.model.ESignatureType.*
import com.blocksdecoded.zrxkit.model.IOrder
import com.blocksdecoded.zrxkit.model.Order
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.sign.eip712.Eip712Data
import com.blocksdecoded.zrxkit.sign.eip712.Eip712Encoder
import com.blocksdecoded.zrxkit.utils.*
import com.blocksdecoded.zrxkit.utils.toHexString
import com.fasterxml.jackson.core.util.ByteArrayBuilder
import java.math.BigInteger
import java.security.SignatureException
import kotlin.experimental.and
import org.bouncycastle.util.encoders.Hex
import org.web3j.crypto.*
import org.web3j.crypto.Sign.recoverFromSignature
import org.web3j.utils.Assertions.verifyPrecondition

class SignUtils {
    private val V_INDEX = 0
    private val R_RANGE = 1..32
    private val S_RANGE = 33..64

    //region Private

    private val types = HashMap<String, List<Eip712Data.Entry>>().apply {
        put("EIP712Domain", listOf(
            Eip712Data.Entry("name", "string"),
            Eip712Data.Entry("version", "string"),
            Eip712Data.Entry("verifyingContract", "address")
        ))

        put("Order", listOf(
            Eip712Data.Entry("makerAddress", "address"),
            Eip712Data.Entry("takerAddress", "address"),
            Eip712Data.Entry("feeRecipientAddress", "address"),
            Eip712Data.Entry("senderAddress", "address"),
            Eip712Data.Entry("makerAssetAmount", "uint256"),
            Eip712Data.Entry("takerAssetAmount", "uint256"),
            Eip712Data.Entry("makerFee", "uint256"),
            Eip712Data.Entry("takerFee", "uint256"),
            Eip712Data.Entry("expirationTimeSeconds", "uint256"),
            Eip712Data.Entry("salt", "uint256"),
            Eip712Data.Entry("makerAssetData", "bytes"),
            Eip712Data.Entry("takerAssetData", "bytes")
        ))
    }

    private fun getDomain(order: IOrder): Eip712Data.EIP712Domain =
        Eip712Data.EIP712Domain("0x Protocol", "2", 0, order.exchangeAddress)

    private fun orderToMap(order: IOrder): HashMap<String, Any> {
        val result = hashMapOf<String, Any>()

        result["makerAddress"] = order.makerAddress
        result["takerAddress"] = order.takerAddress
        result["feeRecipientAddress"] = order.feeRecipientAddress
        result["senderAddress"] = order.senderAddress
        result["makerAssetAmount"] = order.makerAssetAmount.toBigInteger()
        result["takerAssetAmount"] = order.takerAssetAmount.toBigInteger()
        result["makerFee"] = order.makerFee.toBigInteger()
        result["takerFee"] = order.takerFee.toBigInteger()
        result["expirationTimeSeconds"] = order.expirationTimeSeconds.toBigInteger()
        result["salt"] = order.salt.toBigInteger()
        result["makerAssetData"] = order.makerAssetData.clearPrefix().hexStringToByteArray()
        result["takerAssetData"] = order.takerAssetData.clearPrefix().hexStringToByteArray()

        return result
    }

    private fun getOrderSignature(order: IOrder, credentials: Credentials): String {
        val structured = Eip712Data.EIP712Message(
            types,
            "Order",
            orderToMap(order),
            getDomain(order)
        )

        val encoder = Eip712Encoder(structured)

        encoder.validateStructuredData(structured)

        val eipOrder = encoder.hashStructuredData()

        val result = Sign.signMessage(eipOrder, credentials.ecKeyPair, false)

        val builder = ByteArrayBuilder()
        builder.write(result.v.toInt())
        builder.write(result.r)
        builder.write(result.s)
        builder.write(2)

        return builder.toByteArray()
            .toHexString()
            .prefixed()
    }

    private fun rsvFromSignatureHex(signatureHex: String): Sign.SignatureData {
        val decodedSignature = Hex.decode(signatureHex.clearPrefix())

        return Sign.SignatureData(
            decodedSignature[V_INDEX],
            decodedSignature.sliceArray(R_RANGE),
            decodedSignature.sliceArray(S_RANGE)
        )
    }

    @Throws(SignatureException::class)
    internal fun signedMessageHashToKey(messageHash: ByteArray, signatureData: Sign.SignatureData): BigInteger {
        val r = signatureData.r
        val s = signatureData.s
        verifyPrecondition(r != null && r.size == 32, "r must be 32 bytes")
        verifyPrecondition(s != null && s.size == 32, "s must be 32 bytes")

        val header = signatureData.v and 0xFF.toByte()
        // The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
        //                  0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34) {
            throw SignatureException("Header byte out of range: $header")
        }

        val sig = ECDSASignature(
            BigInteger(1, signatureData.r),
            BigInteger(1, signatureData.s)
        )

        val recId = header - 27
        return recoverFromSignature(recId, sig, messageHash)
            ?: throw SignatureException("Could not recover public key from signature")
    }

    //endregion

    //region Public

    fun ecSignOrder(order: Order, credentials: Credentials): SignedOrder? {
        val signature = getOrderSignature(order, credentials)

        val signedOrder = SignedOrder.fromOrder(order, signature)

        return if (isValidSignature(signedOrder))
            signedOrder
        else
            null
    }

    fun isValidSignature(signedOrder: SignedOrder): Boolean {
        val structured = Eip712Data.EIP712Message(
            types,
            "Order",
            orderToMap(signedOrder),
            getDomain(signedOrder)
        )

        val encoder = Eip712Encoder(structured)
        val dataHash = encoder.hashStructuredData()

        val type = getSignatureType(signedOrder.signature)

        var isValid = false
        for (i in 27..28) {
            val restoredVrs = rsvFromSignatureHex(signedOrder.signature)

            val key = when (type) {
                EIP712 -> signedMessageHashToKey(dataHash, restoredVrs)
                ETH_SIGN -> Sign.signedPrefixedMessageToKey(dataHash, restoredVrs)

                WALLET,
                VALIDATOR,
                PRESIGNED,
                NSIGTATURE_TYPES -> TODO("$type signature not implemented")

                ILLEGAL -> throw InvalidSignatureException()
                INVALID -> throw InvalidSignatureException()
            }

            val address = Keys.getAddress(key).prefixed()

            if (address.equals(signedOrder.makerAddress, true)) {
                isValid = true
                break
            }
        }

        return isValid
    }

    fun getSignatureType(signatureHex: String): ESignatureType {
        val type = Hex.decode(signatureHex.clearPrefix()).last().toInt()

        return when (type) {
            2 -> EIP712
            3 -> ETH_SIGN
            else -> throw UnsupportedSignatureType(type)
        }
    }

    //endregion
}
