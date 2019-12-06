package com.fridaytech.zrxkit.sample

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.fridaytech.zrxkit.ZrxKit
import com.fridaytech.zrxkit.contracts.IWethWrapper
import com.fridaytech.zrxkit.contracts.IZrxExchange
import com.fridaytech.zrxkit.model.AssetItem
import com.fridaytech.zrxkit.model.Order
import com.fridaytech.zrxkit.model.SignedOrder
import com.fridaytech.zrxkit.relayer.model.Relayer
import com.fridaytech.zrxkit.relayer.model.RelayerConfig
import com.fridaytech.zrxkit.sample.core.EOrderSide
import com.fridaytech.zrxkit.sample.core.EOrderSide.*
import com.fridaytech.zrxkit.sample.core.Erc20Adapter
import com.fridaytech.zrxkit.sample.core.EthereumAdapter
import com.fridaytech.zrxkit.sample.core.TransactionRecord
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit

class MainViewModel: ViewModel() {

    private val refreshRate = 5L
    private val infuraCredentials = EthereumKit.InfuraCredentials(
        projectId = "0c3f9e6a005b40c58235da423f58b198",
        secretKey = "57b6615fb10b4749a54b29c2894a00df")
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val networkType: EthereumKit.NetworkType = EthereumKit.NetworkType.Ropsten
    private val zrxKitNetworkType: ZrxKit.NetworkType = ZrxKit.NetworkType.Ropsten

    private val feeRecipient = "0xA5004C8b2D64AD08A80d33Ad000820d63aa2cCC9".toLowerCase(Locale.US)

    private val wethAddress = "0xc778417e063141139fce010982780140aa0cd5ab"
    private val tokenAddress = "0x30845a385581ce1dc51d651ff74689d7f4415146"
    private val decimals = 18

    private val gasInfoProvider = object : ZrxKit.GasInfoProvider() {
        override fun getGasLimit(contractFunc: String?): BigInteger =
            400_000.toBigInteger()

        override fun getGasPrice(contractFunc: String?): BigInteger =
            5_000_000_000.toBigInteger() // Gas price 5 GWei
    }

    private val assetPair: Pair<AssetItem, AssetItem>
        get() = zrxKit.relayerManager.availableRelayers.first().availablePairs[0]

    private lateinit var wethContract: IWethWrapper
    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter
    private lateinit var wethAdapter: Erc20Adapter
    private lateinit var tokenAdapter: Erc20Adapter
    private lateinit var zrxKit: ZrxKit
    private lateinit var zrxExchangeContract: IZrxExchange

    private val disposables = CompositeDisposable()

    val ethBalance = MutableLiveData<BigDecimal>()
    val wethBalance = MutableLiveData<BigDecimal>()
    val tokenBalance = MutableLiveData<BigDecimal>()
    val transactions = MutableLiveData<List<TransactionRecord>>()
    val lastBlockHeight = MutableLiveData<Long>()

    val asks = MutableLiveData<List<SignedOrder>>()
    val bids = MutableLiveData<List<SignedOrder>>()

    val alertEvent = SingleLiveEvent<String>()
    val messageEvent = SingleLiveEvent<String>()
    val orderInfoEvent = SingleLiveEvent<Pair<SignedOrder, EOrderSide>>()

    val receiveAddress: String
        get() = ethereumKit.receiveAddress

    private val relayers = listOf(
        Relayer(
            0,
            "BD Relayer",
            listOf(ZrxKit.assetItemForAddress(tokenAddress) to ZrxKit.assetItemForAddress(wethAddress)),
            listOf(feeRecipient),
            zrxKitNetworkType.exchangeAddress,
            RelayerConfig("https://ropsten.api.udex.app", "sra", "v3")
        )
    )

    init {
        init()
    }

    private fun init() {
//        val words = "grocery hedgehog relief fancy pond surprise panic slight clog female deal wash".split(" ")
        val words = "surprise fancy pond panic grocery hedgehog slight relief deal wash clog female".split(" ")

        val seed = Mnemonic().toSeed(words)
        val hdWallet = HDWallet(seed, 1)
        val privateKey = hdWallet.privateKey(0, 0, true).privKey

        ethereumKit = EthereumKit.getInstance(
            App.instance,
            privateKey,
            EthereumKit.SyncMode.ApiSyncMode(),
            networkType,
            infuraCredentials,
            etherscanKey,
            "unique-wallet-id"
        )

        ethereumAdapter = EthereumAdapter(ethereumKit)
        wethAdapter = Erc20Adapter(App.instance, ethereumKit, "Wrapped Eth", "WETH", wethAddress, decimals)
        tokenAdapter = Erc20Adapter(App.instance, ethereumKit, "Tameki Coin V2", "TMKv2", tokenAddress, decimals)

        zrxKit = ZrxKit.getInstance(relayers, privateKey, infuraCredentials.secretKey, zrxKitNetworkType, gasInfoProvider)

        wethContract = zrxKit.getWethWrapperInstance()
        zrxExchangeContract = zrxKit.getExchangeInstance()

        updateEthBalance()
        updateWethBalance()
        updateErc20Balance()
        updateLastBlockHeight()
        refreshOrders()

        //
        // Ethereum
        //

        ethereumAdapter.lastBlockHeightFlowable.subscribe {
            updateLastBlockHeight()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.transactionsFlowable.subscribe {
            updateTransactions()
        }.let {
            disposables.add(it)
        }

        ethereumAdapter.balanceFlowable.subscribe {
            updateEthBalance()
        }.let {
            disposables.add(it)
        }

        //
        // WETH
        //

        wethAdapter.transactionsFlowable.subscribe {
            updateWethTransactions()
        }.let {
            disposables.add(it)
        }

        wethAdapter.balanceFlowable.subscribe {
            updateWethBalance()
        }.let {
            disposables.add(it)
        }

        //
        // ERC20
        //

        tokenAdapter.transactionsFlowable.subscribe {
            updateErc20Transactions()
        }.let {
            disposables.add(it)
        }

        wethAdapter.balanceFlowable.subscribe {
            updateErc20Balance()
        }.let {
            disposables.add(it)
        }

        ethereumKit.start()

        Observable.interval(refreshRate, TimeUnit.SECONDS)
            .subscribe { refreshOrders() }
            .let { disposables.add(it) }
    }

    //region Update

    private fun updateLastBlockHeight() {
        lastBlockHeight.postValue(ethereumKit.lastBlockHeight)
    }

    private fun updateEthBalance() {
        ethBalance.postValue(ethereumAdapter.balance)
    }

    private fun updateWethBalance() {
        wethBalance.postValue(wethAdapter.balance)
    }

    private fun updateErc20Balance() {
        tokenBalance.postValue(tokenAdapter.balance)
    }

    private fun updateTransactions() {
        ethereumAdapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

    private fun updateWethTransactions() {
        wethAdapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

    private fun updateErc20Transactions() {
        tokenAdapter.transactions()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { list: List<TransactionRecord> ->
                    transactions.value = list
                }.let {
                    disposables.add(it)
                }
    }

    private fun refreshOrders() {
        zrxKit.relayerManager
                .getOrderbook(0, assetPair.first.assetData, assetPair.second.assetData)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    asks.value = it.asks.records.map { it.order }
                    bids.value = it.bids.records.map { it.order }
                }, {
                    messageEvent.postValue("Orders refresh error: ${it.message}")
                }).let { disposables.add(it) }
    }

    //endregion

    private fun checkCoinAllowance(address: String): Flowable<Boolean> {
        val coinWrapper = zrxKit.getErc20ProxyInstance(address)

        return coinWrapper.proxyAllowance(receiveAddress)
                .flatMap { Log.d(TAG, "$address allowance $it")
                    if (it > BigInteger.ZERO) {
                        Flowable.just(true)
                    } else {
                        coinWrapper.setUnlimitedProxyAllowance().map {
                            Log.d(TAG, "$address unlocked")
                            true
                        }
                    }
                }
    }

    private fun checkAllowance(): Flowable<Boolean> {
        val base = assetPair.first
        val quote = assetPair.second

        return checkCoinAllowance(base.address)
            .flatMap { checkCoinAllowance(quote.address) }
    }

    //region Public

    fun refresh() {
        ethereumKit.refresh()
    }

    fun wrapEther(amount: BigDecimal) {
        messageEvent.postValue("Wrap started")

        val amountInt = amount.movePointRight(decimals).stripTrailingZeros().toBigInteger()

        wethContract.deposit(amountInt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    alertEvent.postValue("$amount Eth wrapped")
                }, {
                    alertEvent.postValue("Wrap error ${it.message}")
                }).let { disposables.add(it) }
    }

    fun unwrapEther(amount: BigDecimal) {
        messageEvent.postValue("Unwrap started")

        val amountInt = amount.movePointRight(decimals).stripTrailingZeros().toBigInteger()

        wethContract.withdraw(amountInt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    alertEvent.postValue("$amount Weth unwrapped")
                }, {
                    alertEvent.postValue("Unwrap error ${it.message}")
                }).let { disposables.add(it) }
    }

    fun filterTransactions(position: Int) {
        val txMethod = when(position) {
            0 -> ethereumAdapter.transactions()
            1 -> wethAdapter.transactions()
            2 -> tokenAdapter.transactions()
            else -> null
        } ?: return

        txMethod.observeOn(AndroidSchedulers.mainThread())
                .subscribe { txList: List<TransactionRecord> ->
                    transactions.value = txList
                }.let {
                    disposables.add(it)
                }
    }

    //region Order

    fun onOrderClick(position: Int, type: EOrderSide) {
        when(type) {
            ASK -> orderInfoEvent.postValue(asks.value!![position] to type)
            BID -> orderInfoEvent.postValue(bids.value!![position] to type)
        }
    }

    fun fillOrder(order: SignedOrder, side: EOrderSide, amount: BigDecimal) {
        val amountInt = amount.movePointRight(decimals).stripTrailingZeros().toBigInteger()

        checkAllowance().flatMap {
            if (it) {
                when(side) {
                    BID -> zrxExchangeContract.marketBuyOrders(listOf(order), amountInt)
                    ASK -> zrxExchangeContract.marketBuyOrders(listOf(order), amountInt)
                }
            } else {
                Flowable.error(Throwable("Unlock tokens"))
            }
        }?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({
            alertEvent.postValue("Trade confirmed")
            Log.d(TAG, "Fill order hash - $it")
        }, {
            alertEvent.postValue("Trade error:\n${it.message}")
            Log.d(TAG, "Error ${it}")
        })?.let { disposables.add(it) }
    }

    fun createOrder(amount: BigDecimal, price: BigDecimal, side: EOrderSide) {
        val makerAmount = amount.movePointRight(decimals).stripTrailingZeros().toBigInteger()
        val takerAmount = amount.multiply(price).movePointRight(decimals).stripTrailingZeros().toBigInteger()

        Log.d(TAG, "amount $amount price - $price")
        Log.d(TAG, "maker $makerAmount\ntaker - $takerAmount")

        postOrder(makerAmount, takerAmount, side)
    }

    fun postOrder(makeAmount: BigInteger, takeAmount: BigInteger, side: EOrderSide) {
        val expirationTime = ((Date().time / 1000) + (60 * 60 * 24 * 3)).toString() // Order valid for 3 days

        val firstCoinAsset = assetPair.first.assetData
        val secondCoinAsset = assetPair.second.assetData

        val makerAsset = when(side) {
            BID -> secondCoinAsset
            else -> firstCoinAsset
        }

        val takerAsset = when(side) {
            BID -> firstCoinAsset
            else -> secondCoinAsset
        }

        val order = Order(
            chainId = zrxKitNetworkType.id,
            makerAddress = receiveAddress.toLowerCase(),
            exchangeAddress = zrxKitNetworkType.exchangeAddress,
            makerAssetData = makerAsset,
            takerAssetData = takerAsset,
            makerAssetAmount = when (side) {
                BID -> takeAmount.toString()
                else -> makeAmount.toString()
            },
            takerAssetAmount = when (side) {
                BID -> makeAmount.toString()
                else -> takeAmount.toString()
            },
            expirationTimeSeconds = expirationTime,
            senderAddress = "0x0000000000000000000000000000000000000000",
            takerAddress = "0x0000000000000000000000000000000000000000",
            makerFee = "0",
            makerFeeAssetData = makerAsset,
            takerFee = "0",
            takerFeeAssetData = takerAsset,
            feeRecipientAddress = feeRecipient,
            salt = Date().time.toString()
        )

        val signedOrder = zrxKit.signOrder(order)

        if (signedOrder != null) {
            checkAllowance().flatMap {
                if (it) {
                    zrxKit.relayerManager.postOrder(0, signedOrder)
                } else {
                    Flowable.error(Throwable("Unlock tokens"))
                }
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    Log.d(TAG, "Order create error ${it}")
                    alertEvent.postValue("Order create error ${it.message}")
                }, {
                    alertEvent.postValue("Order created")
                }).let { disposables.add(it) }
        }
    }

    //endregion

    //endregion

    companion object {
        private const val TAG = "MainViewModel"
    }
}