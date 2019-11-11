package com.blocksdecoded.zrxkit.relayer.model

import com.blocksdecoded.zrxkit.model.SignedOrder

data class OrderBook(
    val total: Int,
    val page: Int,
    val perPage: Int,
    val records: List<OrderRecord>
)

data class OrderRecord(
    val order: SignedOrder,
    val metaData: OrderMetaData?
)

data class OrderMetaData(
    val orderHash: String?,
    val remainingFillableTakerAssetAmount: String
)
