package com.fridaytech.zrxkit.contracts

import com.fridaytech.zrxkit.model.OrderInfo
import com.fridaytech.zrxkit.model.SignedOrder
import io.reactivex.Flowable
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.protocol.core.methods.response.TransactionReceipt

interface IErc20Proxy {
    fun lockProxy(): Flowable<TransactionReceipt>

    fun setUnlimitedProxyAllowance(): Flowable<TransactionReceipt>

    fun proxyAllowance(ownerAddress: String): Flowable<BigInteger>
}

interface IWethWrapper {
    val depositEstimatedPrice: BigDecimal
    val withdrawEstimatedPrice: BigDecimal
    val depositGasLimit: BigInteger
    val withdrawGasLimit: BigInteger

    fun deposit(amount: BigInteger): Flowable<TransactionReceipt>

    fun withdraw(amount: BigInteger): Flowable<TransactionReceipt>
}

interface IZrxExchange {
    val address: String

    fun marketBuyOrders(orders: List<SignedOrder>, fillAmount: BigInteger): Flowable<String>

    fun marketSellOrders(orders: List<SignedOrder>, fillAmount: BigInteger): Flowable<String>

    fun fillOrder(order: SignedOrder, fillAmount: BigInteger): Flowable<String>

    fun cancelOrder(order: SignedOrder): Flowable<String>

    fun batchCancelOrders(order: List<SignedOrder>): Flowable<String>

    fun ordersInfo(orders: List<SignedOrder>): Flowable<List<OrderInfo>>
}
