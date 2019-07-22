package com.blocksdecoded.zrxkit

import com.blocksdecoded.zrxkit.model.AssetItem
import com.blocksdecoded.zrxkit.model.EAssetProxyId
import com.blocksdecoded.zrxkit.relayer.IRelayerManager
import com.blocksdecoded.zrxkit.relayer.RelayerManager
import com.blocksdecoded.zrxkit.relayer.model.Relayer
import java.math.BigInteger

//TODO: Add 0x components providing feature
class ZrxManager private constructor(
    val relayerManager: IRelayerManager
) {

    companion object {
        private val minAmount = BigInteger("0")
        private val maxAmount = BigInteger("999999999999999999")

        fun init(relayers: List<Relayer>, networkType: NetworkType = NetworkType.KOVAN): ZrxManager {
            val relayerManager = RelayerManager(relayers)

            return ZrxManager(relayerManager)
        }

        fun assetItemForAddress(address: String, type: EAssetProxyId = EAssetProxyId.ERC20): AssetItem = AssetItem(
            minAmount,
            maxAmount,
            address = address,
            type = type
        )
    }

    enum class NetworkType(
        val id: Int
    ) {
        MAINNET(1),
        KOVAN(42)
    }

}