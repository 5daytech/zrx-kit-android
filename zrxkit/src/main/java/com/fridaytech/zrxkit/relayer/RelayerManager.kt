package com.fridaytech.zrxkit.relayer

import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.model.AssetPair
import com.fridaytech.zrxkit.relayer.model.OrderBook
import com.fridaytech.zrxkit.relayer.model.Relayer
import com.fridaytech.zrxkit.relayer.remote.OrderBookResponse
import com.fridaytech.zrxkit.relayer.remote.RelayerApiClient
import io.reactivex.Flowable
import java.util.*

class RelayerManager(
    override val availableRelayers: List<Relayer>
) : IRelayerManager {
    private val relayerClients = availableRelayers.map { RelayerApiClient(it.config) }

    private fun <T> validateRelayerId(relayerId: Int, onValid: () -> Flowable<T>): Flowable<T> =
        if (relayerId < relayerClients.size) {
            onValid.invoke()
        } else {
            Flowable.error(Exception("Relayer index out of bounds - requested: $relayerId, total: ${relayerClients.size}"))
        }

    private fun isValidRetryRelayerId(relayerId: Int) =
        relayerId < relayerClients.size - 1

    override fun getAssetPairs(relayerId: Int): Flowable<List<AssetPair>> =
        validateRelayerId(relayerId) {
            relayerClients[relayerId].getAssets()
                .onErrorResumeNext { throwable: Throwable ->
                    if (isValidRetryRelayerId(relayerId)) {
                        getAssetPairs(relayerId + 1)
                    } else {
                        Flowable.error(throwable)
                    }
                }
        }

    override fun getOrderbook(relayerId: Int, base: String, quote: String, limit: Int): Flowable<OrderBookResponse> =
        validateRelayerId(relayerId) {
            relayerClients[relayerId].getOrderbook(base, quote, limit)
                .onErrorResumeNext { throwable: Throwable ->
                    if (isValidRetryRelayerId(relayerId)) {
                        getOrderbook(relayerId + 1, base, quote, limit)
                    } else {
                        Flowable.error(throwable)
                    }
                }
        }

    override fun postOrder(relayerId: Int, order: SignedOrder): Flowable<Unit> =
        validateRelayerId(relayerId) {
            relayerClients[relayerId].postOrder(order)
                .onErrorResumeNext { throwable: Throwable ->
                    if (isValidRetryRelayerId(relayerId)) {
                        postOrder(relayerId + 1, order)
                    } else {
                        Flowable.error(throwable)
                    }
                }
        }

    override fun getOrders(
        relayerId: Int,
        makerAddress: String?,
        limit: Int
    ): Flowable<OrderBook> = validateRelayerId(relayerId) {
        relayerClients[relayerId].getOrders(
            makerAddress?.toLowerCase(Locale.US),
            limit
        ).onErrorResumeNext { throwable: Throwable ->
            if (isValidRetryRelayerId(relayerId)) {
                getOrders(relayerId + 1, makerAddress, limit)
            } else {
                Flowable.error(throwable)
            }
        }
    }
}
