package com.blocksdecoded.zrxkit.contracts

import com.blocksdecoded.zrxkit.clearPrefix
import com.blocksdecoded.zrxkit.contracts.StructFunctionEncoder.EFunction.*
import com.blocksdecoded.zrxkit.decodePrefixedHex
import com.blocksdecoded.zrxkit.hexStringToByteArray
import com.blocksdecoded.zrxkit.model.OrderInfo
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.prefixed
import com.esaulpaugh.headlong.abi.Function
import com.esaulpaugh.headlong.abi.Tuple
import org.bouncycastle.util.encoders.Hex
import org.web3j.abi.datatypes.Address
import org.web3j.crypto.RawTransaction
import org.web3j.utils.Numeric
import java.math.BigInteger

internal object StructFunctionEncoder {

    internal const val ORDER_SIGNATURE: String = "(address,address,address,address,uint256,uint256,uint256,uint256,uint256,uint256,bytes,bytes)"
    internal const val ORDER_INFO_SIGNATURE: String = "(uint256,bytes32,uint256)"

    //region Private

    private fun getTupleFromOrder(order: SignedOrder): Tuple = Tuple(
        Address(order.makerAddress).toUint160().value,
        Address(order.takerAddress).toUint160().value,
        Address(order.feeRecipientAddress).toUint160().value,
        Address(order.senderAddress).toUint160().value,
        order.makerAssetAmount.toBigInteger(),
        order.takerAssetAmount.toBigInteger(),
        order.makerFee.toBigInteger(),
        order.takerFee.toBigInteger(),
        order.expirationTimeSeconds.toBigInteger(),
        order.salt.toBigInteger(),
        order.makerAssetData.clearPrefix().hexStringToByteArray(),
        order.takerAssetData.clearPrefix().hexStringToByteArray()
    )

    private fun encodeData(args: List<Any>): Tuple {
        val items = ArrayList<Any>()

        args.forEach {
            val element = when(it) {
                is SignedOrder -> getTupleFromOrder(it)

                is String -> it.decodePrefixedHex()

                is Collection<*> -> {
                    when {
                        it.first() is SignedOrder -> (it as Collection<SignedOrder>).map {
                            getTupleFromOrder(it)
                        }.toTypedArray()

                        it.first() is String -> (it as Collection<String>).map { it.decodePrefixedHex() }.toTypedArray()
                        else -> it
                    }
                }

                else -> it
            }

            items.add(element)
        }

        return Tuple(*items.toTypedArray())
    }

    private fun encodeFunction(type: EFunction, data: List<Any>, prefixed: Boolean = true): String {
        val buffer = type.function.encodeCall(encodeData(data))

        return if (prefixed)
            Numeric.toHexString(buffer.array()).prefixed()
        else
            Numeric.toHexString(buffer.array())
    }

    private fun getRawTransaction(nonce: BigInteger, to: String, data: String): RawTransaction =
        RawTransaction.createTransaction(
            nonce,
            5_000_000_000.toBigInteger(),
            400_000.toBigInteger(),
            to,
            data
        )

    //endregion

    //region Public

    fun getMarketBuyOrdersTransaction(nonce: BigInteger, orders: List<SignedOrder>, fillAmount: BigInteger): RawTransaction {
        val data = encodeFunction(
            MARKET_BUY_ORDERS,
            listOf(orders, fillAmount, orders.map { it.signature })
        )

        return getRawTransaction(
            nonce,
            orders.first().exchangeAddress,
            data
        )
    }

    fun getCancelOrderTransaction(nonce: BigInteger, order: SignedOrder): RawTransaction =
        getRawTransaction(
            nonce,
            order.exchangeAddress,
            encodeFunction(
                CANCEL_ORDER,
                listOf(order)
            )
        )

    fun getFillOrderTransaction(nonce: BigInteger, order: SignedOrder, fillAmount: BigInteger): RawTransaction {
        val data = encodeFunction(
            FILL_ORDER,
            listOf(order, fillAmount, order.signature)
        )

        return getRawTransaction(
            nonce,
            order.exchangeAddress,
            data
        )
    }

    fun getMarketSellOrdersTransaction(nonce: BigInteger, orders: List<SignedOrder>, fillAmount: BigInteger): RawTransaction {
        val data = encodeFunction(
            MARKET_SELL_ORDERS,
            listOf(orders, fillAmount, orders.map { it.signature })
        )

        return getRawTransaction(
            nonce,
            orders.first().exchangeAddress,
            data
        )
    }

    fun encodedOrdersInfoData(orders: List<SignedOrder>) : String =
        encodeFunction(ORDERS_INFO, listOf(orders), prefixed = false)

    fun decodeOrdersInfo(data: String): List<OrderInfo> {
        val value = data.substring(2)

        val decoded = ORDERS_INFO.function.decodeReturn(Hex.decode(value))

        val result = ArrayList<OrderInfo>()

        decoded.forEach {
            (it as? Array<Tuple>)?.forEach {
                result.add(
                    OrderInfo(
                        it[0].toString(),
                        Hex.toHexString(it[1] as ByteArray),
                        BigInteger(it[2].toString())
                    )
                )
            }
        }

        return result
    }

    //endregion

    enum class EFunction(
        private val signature: String,
        private val outputs: String = ""
    ) {
        CANCEL_ORDER("cancelOrder($ORDER_SIGNATURE)"),

        ORDERS_INFO(
            "getOrdersInfo($ORDER_SIGNATURE[])",
            "($ORDER_INFO_SIGNATURE[])"
        ),

        MARKET_SELL_ORDERS("marketSellOrders($ORDER_SIGNATURE[],uint256,bytes[])"),

        MARKET_BUY_ORDERS("marketBuyOrders($ORDER_SIGNATURE[],uint256,bytes[])"),

        FILL_ORDER("fillOrder($ORDER_SIGNATURE,uint256,bytes)");

        val function: Function
            get() = if (outputs.isEmpty()) {
                Function(signature)
            } else {
                Function(signature, outputs)
            }
    }
}