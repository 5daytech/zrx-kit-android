package com.blocksdecoded.zrxkit.contracts

import com.blocksdecoded.zrxkit.model.OrderInfo
import com.blocksdecoded.zrxkit.model.SignedOrder
import io.reactivex.Flowable
import org.web3j.crypto.Credentials
import org.web3j.crypto.RawTransaction
import org.web3j.crypto.TransactionEncoder
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Contract
import org.web3j.tx.gas.ContractGasProvider
import org.web3j.utils.Numeric
import java.math.BigInteger

class ZrxExchangeWrapper(
    contractAddress: String,
    var credentials: Credentials,
    contractGasProvider: ContractGasProvider,
    providerUrl: String
) : Contract(BINARY, contractAddress, Web3j.build(HttpService(providerUrl)), credentials, contractGasProvider) {

    //region Private

    private fun getNonce(): Flowable<BigInteger> =
        web3j.ethGetTransactionCount(credentials.address, defaultBlockParameter)
            .flowable().map { it.transactionCount }

    private fun RawTransaction.sign(): String {
        val signedMessage = TransactionEncoder.signMessage(this, credentials)
        return Numeric.toHexString(signedMessage)
    }

    private inline fun <reified T> sendTransaction(transaction: RawTransaction): Flowable<T> =
        web3j.ethSendRawTransaction(transaction.sign())
            .flowable()
            .flatMap {
                if (it.hasError()) {
                    Flowable.error(Throwable(it.error.message))
                } else {
                    if (it.transactionHash is T) {
                        Flowable.just(it.transactionHash as T)
                    } else {
                        Flowable.error(Throwable("Incorrect return type - expected ${T::class.java.simpleName}"))
                    }
                }
            }

    //endregion

    //region Public

    fun cancelOrder(order: SignedOrder): Flowable<String> {
        return getNonce().flatMap {
            val transaction = StructFunctionEncoder.getCancelOrderTransaction(it, order)

            sendTransaction<String>(transaction)
        }
    }

    fun fillOrder(order: SignedOrder, fillAmount: BigInteger): Flowable<String> {
        return getNonce().flatMap {
            val transaction = StructFunctionEncoder.getFillOrderTransaction(
                it,
                order,
                fillAmount
            )

            sendTransaction<String>(transaction)
        }
    }

    fun marketBuyOrders(orders: List<SignedOrder>, fillAmount: BigInteger): Flowable<String> {
        return getNonce().flatMap {
            val transaction = StructFunctionEncoder.getMarketBuyOrdersTransaction(
                it,
                orders,
                fillAmount
            )

            sendTransaction<String>(transaction)
        }
    }

    fun marketSellOrders(orders: List<SignedOrder>, fillAmount: BigInteger): Flowable<String> {
        return getNonce().flatMap {
            val transaction = StructFunctionEncoder.getMarketSellOrdersTransaction(
                it,
                orders,
                fillAmount
            )

            sendTransaction<String>(transaction)
        }
    }

    fun ordersInfo(orders: List<SignedOrder>): Flowable<List<OrderInfo>> {
        val transaction = Transaction.createEthCallTransaction(
            credentials.address,
            contractAddress,
            StructFunctionEncoder.encodedOrdersInfoData(orders)
        )

        return web3j.ethCall(transaction, defaultBlockParameter)
            .flowable()
            .map { StructFunctionEncoder.decodeOrdersInfo(it.value) }
    }

    //endregion

    companion object {
        private const val BINARY = ""
    }
}