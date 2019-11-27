package com.fridaytech.zrxkit.relayer

import com.fridaytech.zrxkit.ZrxKit
import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.model.AssetPair
import com.fridaytech.zrxkit.relayer.model.OrderBook
import com.fridaytech.zrxkit.relayer.model.Relayer
import com.fridaytech.zrxkit.relayer.remote.OrderBookResponse
import com.fridaytech.zrxkit.relayer.remote.RelayerApiClient
import io.reactivex.Flowable
import java.util.*

class RelayerManager(
    override val availableRelayers: List<Relayer>,
    private val networkType: ZrxKit.NetworkType
) : IRelayerManager {
    private val relayerClients = availableRelayers.map { RelayerApiClient(it.config) }

    override fun getAssetPairs(relayerId: Int): Flowable<List<AssetPair>> =
        relayerClients[relayerId].getAssets()

    override fun getOrderbook(relayerId: Int, base: String, quote: String, limit: Int): Flowable<OrderBookResponse> =
        relayerClients[relayerId].getOrderbook(base, quote, limit)

    override fun postOrder(relayerId: Int, order: SignedOrder): Flowable<Unit> =
        relayerClients[relayerId].postOrder(order)

    override fun getOrders(
        relayerId: Int,
        makerAddress: String?,
        limit: Int
    ): Flowable<OrderBook> = relayerClients[relayerId].getOrders(
        makerAddress?.toLowerCase(Locale.US),
        limit
    )
}
