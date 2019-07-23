package com.blocksdecoded.zrxkit

import com.blocksdecoded.zrxkit.contracts.Erc20ProxyWrapper
import com.blocksdecoded.zrxkit.contracts.WethWrapper
import com.blocksdecoded.zrxkit.contracts.ZrxExchangeWrapper
import com.blocksdecoded.zrxkit.model.AssetItem
import com.blocksdecoded.zrxkit.model.EAssetProxyId
import com.blocksdecoded.zrxkit.model.Order
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.relayer.IRelayerManager
import com.blocksdecoded.zrxkit.relayer.RelayerManager
import com.blocksdecoded.zrxkit.relayer.model.Relayer
import com.blocksdecoded.zrxkit.utils.SignUtils
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class ZrxKit private constructor(
    val relayerManager: IRelayerManager,
    private val credentials: Credentials,
    private val gasInfoProvider: ContractGasProvider,
    private val providerUrl: String
) {

    fun getWethWrapperInstance(address: String): WethWrapper =
        WethWrapper(address, credentials, gasInfoProvider, providerUrl)

    fun getExchangeInstance(address: String): ZrxExchangeWrapper =
        ZrxExchangeWrapper(address, credentials, gasInfoProvider, providerUrl)

    fun getErcProxyInstance(address: String): Erc20ProxyWrapper =
        Erc20ProxyWrapper(address, credentials, gasInfoProvider, providerUrl)

    fun signOrder(order: Order): SignedOrder? = SignUtils().ecSignOrder(order, credentials)

    companion object {
        private val minAmount = BigInteger("0")

        private val maxAmount = BigInteger("999999999999999999")

        fun getInstance(
            relayers: List<Relayer>,
            privateKey: BigInteger,
            gasPriceProvider: GasInfoProvider,
            infuraKey: String,
            networkType: NetworkType = NetworkType.KOVAN
        ): ZrxKit {
            val relayerManager = RelayerManager(relayers)
            val credentials = Credentials.create(ECKeyPair.create(privateKey))

            return ZrxKit(relayerManager, credentials, gasPriceProvider, networkType.getInfuraUrl(infuraKey))
        }

        fun assetItemForAddress(address: String, type: EAssetProxyId = EAssetProxyId.ERC20): AssetItem = AssetItem(
            minAmount,
            maxAmount,
            address = address,
            type = type
        )
    }

    enum class NetworkType(
        val id: Int,
        private val subdomain: String
    ) {
        MAINNET(1, "mainnet"),
        KOVAN(42, "kovan");

        fun getInfuraUrl(infuraKey: String): String {
            return "https://$subdomain.infura.io/$infuraKey"
        }
    }

    abstract class GasInfoProvider: ContractGasProvider {
        override fun getGasLimit(): BigInteger = getGasLimit("")
        override fun getGasPrice(): BigInteger = getGasPrice("")
    }
}