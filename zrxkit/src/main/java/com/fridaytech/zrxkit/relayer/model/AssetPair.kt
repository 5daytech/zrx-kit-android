package com.fridaytech.zrxkit.relayer.model

import java.math.BigInteger

data class AssetPairsResponse(
    val total: Int,
    val page: Int,
    val perPage: Int,
    val records: List<AssetPair>
)

data class AssetPair(
    val assetDataA: Asset,
    val assetDataB: Asset
)

data class Asset(
    val minAmount: Int,
    val maxAmount: BigInteger,
    val assetData: String,
    val precision: Int
)
