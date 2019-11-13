package com.fridaytech.zrxkit.utils.eip712

import com.fridaytech.zrxkit.model.ESignatureType
import com.fridaytech.zrxkit.model.Order
import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.sign.SignUtils
import com.fridaytech.zrxkit.sign.eip712.Eip712Data
import com.fridaytech.zrxkit.sign.eip712.Eip712Encoder
import com.fridaytech.zrxkit.utils.toHexString
import org.junit.Assert.*
import org.junit.Test
import org.web3j.crypto.*

class Eip712EncoderTest {

    private val domain = Eip712Data.EIP712Domain(
        "Ether Mail",
        "1",
        1,
        "0xCcCCccccCCCCcCCCCCCcCcCccCcCCCcCcccccccC"
    )

    inner class Person(
        val name: String,
        val wallet: String
    ) {
        fun toMap(): HashMap<String, Any> {
            val result = HashMap<String, Any>()

            result["name"] = name
            result["wallet"] = wallet

            return result
        }
    }

    private fun getMessage(): HashMap<String, Any> {
        val result = hashMapOf<String, Any>()

        result["from"] = Person("Cow", "0xCD2a3d9F938E13CD947Ec05AbC7FE734Df8DD826").toMap()
        result["to"] = Person("Bob", "0xbBbBBBBbbBBBbbbBbbBbbbbBBbBbbbbBbBbbBBbB").toMap()
        result["contents"] = "Hello, Bob!"

        return result
    }

    private val types = HashMap<String, List<Eip712Data.Entry>>().apply {
        put("EIP712Domain", listOf(
            Eip712Data.Entry("name", "string"),
            Eip712Data.Entry("version", "string"),
            Eip712Data.Entry("chainId", "uint256"),
            Eip712Data.Entry("verifyingContract", "address")
        ))

        put("Mail", listOf(
            Eip712Data.Entry("from", "Person"),
            Eip712Data.Entry("to", "Person"),
            Eip712Data.Entry("contents", "string")
        ))

        put("Person", listOf(
            Eip712Data.Entry("name", "string"),
            Eip712Data.Entry("wallet", "address")
        ))
    }

    private val private: ByteArray = Hash.sha3("cow".toByteArray())
    private val keys: ECKeyPair = ECKeyPair.create(private)

    private val message = Eip712Data.EIP712Message(
        types, "Mail", getMessage(), domain
    )

    @Test
    fun `Mail sign`() {
        val encoder = Eip712Encoder(message)

        // Check type encoding
        assertEquals("Mail(Person from,Person to,string contents)Person(string name,address wallet)",
            encoder.encodeType("Mail"))

        // Check type hash
        assertEquals("0xa0cedeb2dc280ba39b857546d74f5549c3a1d7bdc2dd96bf881f76108e23dac2",
            "0x${encoder.hashType("Mail").toHexString()}")

        // Check type data encoding
        assertEquals("0xa0cedeb2dc280ba39b857546d74f5549c3a1d7bdc2dd96bf881f76108e23dac2fc71e5fa27ff56c350aa531bc129ebdf613b772b6604664f5d8dbe21b85eb0c8cd54f074a4af31b4411ff6a60c9719dbd559c221c8ac3492d9d872b041d703d1b5aadf3154a261abdd9086fc627b61efca26ae5702701d05cd2305f7c52a2fc8",
            "0x${encoder.encodeData(message.primaryType, getMessage()).toHexString()}")

        // Check type data hash
        assertEquals("0xc52c0ee5d84264471806290a3f2c4cecfc5490626bf912d01f240d7a274b371e",
            "0x${encoder.hashMessage(message.primaryType, getMessage()).toHexString()}")

        // Check domain hash
        assertEquals("0xf2cee375fa42b42143804025fc449deafd50cc031ca257e0b194a650a912090f",
            "0x${encoder.hashDomain().toHexString()}")

        // Check hash sign
        assertEquals("0xbe609aee343fb3c4b28e1df9e632fca64fcfaede20f02e86244efddf30957bd2",
            "0x${encoder.hashStructuredData().toHexString()}")

        assertEquals("0xcd2a3d9f938e13cd947ec05abc7fe734df8dd826", "0x${Keys.getAddress(keys)}")

        val sign = Sign.signMessage(encoder.hashStructuredData(), keys, false)

        assertEquals(28.toByte(), sign.v)
        assertEquals("0x4355c47d63924e8a72e509b65029052eb6c299d53a04e167c5775fd466751c9d", "0x${sign.r.toHexString()}")
        assertEquals("0x07299936d304c153f6443dfa05f40ff007d72911b6f72307f996231605b91562", "0x${sign.s.toHexString()}")
    }

    @Test
    fun `Order sign`() {
        val cred = Credentials.create("26A5D2061D8D958ADF0B3A0BBD1A58338BC11865BE26B27B56256FE13732E090")

        assertEquals("0xe2507b493bef003030f0a053d55af80237a44c64", cred.address)

        val order = Order(
            makerAddress = cred.address.toLowerCase(),
            exchangeAddress = "0x35dd2932454449b14cee11a94d3674a936d5d7b2",
            makerAssetData = "0xf47261b00000000000000000000000002002d3812f58e35f0ea1ffbf80a75a38c32175fa",
            takerAssetData = "0xf47261b0000000000000000000000000d0a1e359811322d97991e03f863a0c30c2cf029c",
            makerAssetAmount = "10000000000000000000",
            takerAssetAmount = "10000000000000000",
            expirationTimeSeconds = "1561628788",
            senderAddress = "0x0000000000000000000000000000000000000000",
            takerAddress = "0x0000000000000000000000000000000000000000",
            makerFee = "0",
            takerFee = "0",
            feeRecipientAddress = "0xa258b39954cef5cb142fd567a46cddb31a670124",
            salt = "1561542388954"
        )

        val signedOrder = SignUtils().ecSignOrder(order, cred)

        assertEquals("0x1cdc0509ab7dab796f6aab6d1cf2feb2a6c769483a4861dcccc016a5050a13b9354e6c0e54b91dec84893794eea275312cb0042ef1ea9b1f640f79bf4cb20782fb02",
            signedOrder?.signature)
    }

    @Test
    fun `Order signature validate`() {
        val order = SignedOrder(
            exchangeAddress = "0x35dd2932454449b14cee11a94d3674a936d5d7b2",
            makerAssetData = "0xf47261b0000000000000000000000000d0a1e359811322d97991e03f863a0c30c2cf029c",
            takerAssetData = "0xf47261b00000000000000000000000002002d3812f58e35f0ea1ffbf80a75a38c32175fa",
            makerAssetAmount = "374999999999999999",
            takerAssetAmount = "404007756948933419521",
            makerAddress = "0x7f06340c5ca1b5b3648cc1c3c290aa5851ddb7f7",
            takerAddress = "0x0000000000000000000000000000000000000000",
            expirationTimeSeconds = "1561545792",
            senderAddress = "0x0000000000000000000000000000000000000000",
            feeRecipientAddress = "0xa258b39954cef5cb142fd567a46cddb31a670124",
            makerFee = "0",
            takerFee = "0",
            salt = "1561545552952",
            signature = "0x1cec7bc3eb323169417bc33eeaea77a3b8bd79a52f284de4531d352c9b9e00e92464c6dffb04b2595a65030cb7f3869da65bfcd4293303c261d4a4c577c0c9b90003"
        )

        val signer = SignUtils()
        assertEquals(signer.getSignatureType(order.signature), ESignatureType.ETH_SIGN)
        assertTrue(signer.isValidSignature(order))
    }
}
