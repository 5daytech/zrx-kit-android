package com.blocksdecoded.zrxkit

import com.blocksdecoded.zrxkit.relayer.IRelayerManager
import com.blocksdecoded.zrxkit.relayer.RelayerManager
import com.blocksdecoded.zrxkit.relayer.model.Relayer

class ZrxManager private constructor(
    val relayerManager: IRelayerManager
) {

    companion object {
        fun init(relayers: List<Relayer>, networkId: Int = 42): ZrxManager {
            val relayerManager = RelayerManager(relayers)

            return ZrxManager(relayerManager)
        }
    }

}