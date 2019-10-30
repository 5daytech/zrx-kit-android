package com.blocksdecoded.zrxkit.relayer.remote

import com.blocksdecoded.zrxkit.relayer.model.OrderBook

data class OrderBookResponse(
    val bids: OrderBook,
    val asks: OrderBook
)
