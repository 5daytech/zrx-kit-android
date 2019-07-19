package com.blocksdecoded.zrxkit.relayer

import com.blocksdecoded.zrxkit.relayer.model.AssetPair
import com.blocksdecoded.zrxkit.relayer.remote.OrderBookResponse
import com.blocksdecoded.zrxkit.relayer.remote.RelayerApiClient
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.relayer.model.Relayer
import io.reactivex.Flowable

class RelayerManager(
    override val availableRelayers: List<Relayer>
) : IRelayerManager {
    private val relayerClients = availableRelayers.map { RelayerApiClient(it.config) }

    override fun getAssetPairs(relayerId: Int): Flowable<List<AssetPair>> =
        relayerClients[relayerId].getAssets()

    override fun getOrderbook(relayerId: Int, base: String, quote: String): Flowable<OrderBookResponse> =
        relayerClients[relayerId].getOrderbook(base, quote)

    override fun postOrder(relayerId: Int, order: SignedOrder): Flowable<Unit> =
        relayerClients[relayerId].postOrder(order)

    override fun getOrders(
        relayerId: Int,
        makerAddress: String,
        makerAsset: String,
        takerAsset: String
    ): Flowable<OrderBookResponse> {
        TODO("Get orders not implemented")
    }
}