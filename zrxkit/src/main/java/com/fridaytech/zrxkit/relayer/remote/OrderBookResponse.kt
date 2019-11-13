package com.fridaytech.zrxkit.relayer.remote

import com.fridaytech.zrxkit.relayer.model.OrderBook

data class OrderBookResponse(
    val bids: OrderBook,
    val asks: OrderBook
)
