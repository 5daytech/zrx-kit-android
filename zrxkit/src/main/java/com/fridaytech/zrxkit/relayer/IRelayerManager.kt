package com.fridaytech.zrxkit.relayer

import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.model.AssetPair
import com.fridaytech.zrxkit.relayer.model.OrderBook
import com.fridaytech.zrxkit.relayer.model.Relayer
import com.fridaytech.zrxkit.relayer.remote.OrderBookResponse
import io.reactivex.Flowable

interface IRelayerManager {
    val availableRelayers: List<Relayer>

    fun getAssetPairs(relayerId: Int): Flowable<List<AssetPair>>

    fun getOrderbook(
        relayerId: Int,
        base: String,
        quote: String,
        limit: Int = 100
    ): Flowable<OrderBookResponse>

    fun postOrder(relayerId: Int, order: SignedOrder): Flowable<Unit>

    fun getOrders(
        relayerId: Int,
        makerAddress: String? = null,
        limit: Int = 100
    ): Flowable<OrderBook>
}
