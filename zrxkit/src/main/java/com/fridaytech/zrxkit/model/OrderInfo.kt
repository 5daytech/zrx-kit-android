package com.fridaytech.zrxkit.model

import java.math.BigInteger

data class OrderInfo(
    val orderStatus: String,
    val orderHash: String,
    val orderTakerAssetFilledAmount: BigInteger
)
