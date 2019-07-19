package com.blocksdecoded.zrxkit.model

import java.math.BigInteger

data class AssetItem(
    val minAmount: BigInteger,
    val maxAmount: BigInteger,
    val address: String,
    val type: EAssetProxyId,
    val assetData: String = type.encode(address)
)