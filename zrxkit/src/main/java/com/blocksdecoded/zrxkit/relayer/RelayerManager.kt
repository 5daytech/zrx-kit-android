package com.blocksdecoded.zrxkit.relayer

import com.blocksdecoded.zrxkit.ZrxKit
import com.blocksdecoded.zrxkit.relayer.model.AssetPair
import com.blocksdecoded.zrxkit.relayer.remote.OrderBookResponse
import com.blocksdecoded.zrxkit.relayer.remote.RelayerApiClient
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.relayer.model.Relayer
import io.reactivex.Flowable

class RelayerManager(
    override val availableRelayers: List<Relayer>,
    private val networkType: ZrxKit.NetworkType
) : IRelayerManager {
    private val relayerClients = availableRelayers.map { RelayerApiClient(it.config) }

    override fun getAssetPairs(relayerId: Int): Flowable<List<AssetPair>> =
        relayerClients[relayerId].getAssets(networkId = networkType.id)

    override fun getOrderbook(relayerId: Int, base: String, quote: String): Flowable<OrderBookResponse> =
        relayerClients[relayerId].getOrderbook(base, quote, networkId = networkType.id)

    override fun postOrder(relayerId: Int, order: SignedOrder): Flowable<Unit> =
        relayerClients[relayerId].postOrder(order, networkId = networkType.id)

    override fun getOrders(
        relayerId: Int,
        makerAddress: String,
        makerAsset: String,
        takerAsset: String
    ): Flowable<OrderBookResponse> {
        TODO("Get orders not implemented")
    }
}