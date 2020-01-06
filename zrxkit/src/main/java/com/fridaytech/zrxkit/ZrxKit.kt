package com.fridaytech.zrxkit

import com.fridaytech.zrxkit.contracts.*
import com.fridaytech.zrxkit.model.AssetItem
import com.fridaytech.zrxkit.model.EAssetProxyId
import com.fridaytech.zrxkit.model.Order
import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.IRelayerManager
import com.fridaytech.zrxkit.relayer.RelayerManager
import com.fridaytech.zrxkit.relayer.model.Relayer
import com.fridaytech.zrxkit.sign.SignUtils
import com.fridaytech.zrxkit.utils.CoreUtils
import com.fridaytech.zrxkit.utils.toEther
import io.reactivex.Flowable
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.tx.gas.ContractGasProvider

class ZrxKit private constructor(
    val relayerManager: IRelayerManager,
    private val credentials: Credentials,
    private val providerUrl: String,
    private val networkType: NetworkType,
    private val gasInfoProvider: ContractGasProvider
) {

    fun getWethWrapperInstance(wrapperAddress: String = networkType.wethAddress): IWethWrapper =
        WethWrapper(wrapperAddress, credentials, gasInfoProvider, providerUrl)

    fun getExchangeInstance(address: String = networkType.exchangeAddress): IZrxExchange =
        ZrxExchangeWrapper(address, credentials, gasInfoProvider, providerUrl)

    fun getErc20ProxyInstance(tokenAddress: String, proxyAddress: String = networkType.erc20ProxyAddress): IErc20Proxy =
        Erc20ProxyWrapper(tokenAddress, credentials, gasInfoProvider, providerUrl, proxyAddress)

    fun signOrder(order: Order): SignedOrder? = SignUtils().ecSignOrder(order, credentials)

    fun protocolFeeInEth(fillOrdersCount: Int): BigDecimal = protocolFee(fillOrdersCount).toEther()

    fun protocolFee(fillOrdersCount: Int): BigInteger =
        CoreUtils.getProtocolFee(gasInfoProvider, fillOrdersCount)

    val marketBuyEstimatedPrice: Flowable<BigDecimal>
        get() {
            val price = gasInfoProvider.getGasLimit("marketBuyOrders") *
                    gasInfoProvider.getGasPrice("marketBuyOrders")

            return Flowable.just(price.toEther())
        }

    companion object {
        private val defaultGasProvider: GasInfoProvider = object : ZrxKit.GasInfoProvider() {}

        private val minAmount = BigInteger("0")

        private val maxAmount = BigInteger("999999999999999999")

        fun getInstance(
            relayers: List<Relayer>,
            privateKey: BigInteger,
            infuraKey: String,
            networkType: NetworkType = NetworkType.Ropsten,
            gasPriceProvider: GasInfoProvider = defaultGasProvider
        ): ZrxKit {
            val relayerManager = RelayerManager(relayers)
            val credentials = Credentials.create(ECKeyPair.create(privateKey))

            return ZrxKit(relayerManager, credentials, networkType.getInfuraUrl(infuraKey), networkType, gasPriceProvider)
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
        val exchangeAddress: String,
        val erc20ProxyAddress: String,
        val erc721ProxyAddress: String,
        val wethAddress: String,
        private val subdomain: String
    ) {
        MainNet(
            1,
            "0x61935cbdd02287b511119ddb11aeb42f1593b7ef",
            "0x95e6f48254609a6ee006f7d493c8e5fb97094cef",
            "0xefc70a1b18c432bdc64b596838b4d138f6bc6cad",
            "0xc02aaa39b223fe8d0a0e5c4f27ead9083c756cc2",
            "mainnet"
        ),
        Ropsten(
            3,
            "0xfb2dd2a1366de37f7241c83d47da58fd503e2c64",
            "0xb1408f4c245a23c31b98d2c626777d4c0d766caa",
            "0xe654aac058bfbf9f83fcaee7793311dd82f6ddb4",
            "0xc778417e063141139fce010982780140aa0cd5ab",
            "ropsten"
        ),
        Kovan(
            42,
            "0x4eacd0af335451709e1e7b570b8ea68edec8bc97",
            "0xf1ec01d6236d3cd881a0bf0130ea25fe4234003e",
            "0x2a9127c745688a165106c11cd4d647d2220af821",
            "0xd0a1e359811322d97991e03f863a0c30c2cf029c",
            "kovan"
        );

        fun getInfuraUrl(infuraKey: String): String {
            return "https://$subdomain.infura.io/$infuraKey"
        }
    }

    abstract class GasInfoProvider : ContractGasProvider {
        override fun getGasLimit(contractFunc: String?): BigInteger = when (contractFunc) {
            WethWrapper.FUNC_DEPOSIT -> 40000.toBigInteger()
            WethWrapper.FUNC_WITHDRAW -> 60000.toBigInteger()
            Erc20ProxyWrapper.FUNC_APPROVE -> 80000.toBigInteger()
            else -> 400_000.toBigInteger()
        }

        override fun getGasPrice(contractFunc: String?): BigInteger = 5_000_000_000L.toBigInteger()

        override fun getGasLimit(): BigInteger = getGasLimit("")
        override fun getGasPrice(): BigInteger = getGasPrice("")
    }
}
