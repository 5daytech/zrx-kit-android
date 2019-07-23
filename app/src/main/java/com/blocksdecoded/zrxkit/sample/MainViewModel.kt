package com.blocksdecoded.zrxkit.sample

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blocksdecoded.zrxkit.ZrxKit
import com.blocksdecoded.zrxkit.contracts.WethWrapper
import com.blocksdecoded.zrxkit.contracts.ZrxExchangeWrapper
import com.blocksdecoded.zrxkit.model.AssetItem
import com.blocksdecoded.zrxkit.model.Order
import com.blocksdecoded.zrxkit.model.SignedOrder
import com.blocksdecoded.zrxkit.relayer.model.Relayer
import com.blocksdecoded.zrxkit.relayer.model.RelayerConfig
import com.blocksdecoded.zrxkit.sample.core.EOrderSide
import com.blocksdecoded.zrxkit.sample.core.EOrderSide.*
import com.blocksdecoded.zrxkit.sample.core.Erc20Adapter
import com.blocksdecoded.zrxkit.sample.core.EthereumAdapter
import com.blocksdecoded.zrxkit.sample.core.TransactionRecord
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
        projectId = "2a1306f1d12f4c109a4d4fb9be46b02e",
        secretKey = "fc479a9290b64a84a15fa6544a130218")
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val infuraKey = "57b6615fb10b4749a54b29c2894a00df"
    private val networkType: EthereumKit.NetworkType = EthereumKit.NetworkType.Kovan

    private val feeRecipient = "0x2e8da0868e46fc943766a98b8d92a0380b29ce2a"

    private val exchangeAddress = "0x30589010550762d2f0d06f650d8e8b6ade6dbf4b"
    private val wethAddress = "0xd0a1e359811322d97991e03f863a0c30c2cf029c"
    private val zrxAddress = "0x2002d3812f58e35f0ea1ffbf80a75a38c32175fa"
    private val decimals = 18

    private val gasInfoProvider = object : ZrxKit.GasInfoProvider() {
        override fun getGasLimit(contractFunc: String?): BigInteger =
            200_000.toBigInteger()

        override fun getGasPrice(contractFunc: String?): BigInteger =
            5_000_000_000.toBigInteger() // Gas price 5 GWei
    }

    private val assetPair: Pair<AssetItem, AssetItem>
        get() = zrxKit.relayerManager.availableRelayers.first().availablePairs[0]

    private lateinit var wethContract: WethWrapper
    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter
    private lateinit var wethAdapter: Erc20Adapter
    private lateinit var zrxAdapter: Erc20Adapter
    private lateinit var zrxKit: ZrxKit
    private lateinit var zrxExchangeContract: ZrxExchangeWrapper

    private val disposables = CompositeDisposable()

    val ethBalance = MutableLiveData<BigDecimal>()
    val wethBalance = MutableLiveData<BigDecimal>()
    val zrxBalance = MutableLiveData<BigDecimal>()
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
            listOf(ZrxKit.assetItemForAddress(zrxAddress) to ZrxKit.assetItemForAddress(wethAddress)),
            listOf(feeRecipient),
            exchangeAddress,
            RelayerConfig("http://relayer.staging.fridayte.ch", "", "v2")
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
        zrxAdapter = Erc20Adapter(App.instance, ethereumKit, "0x", "ZRX", zrxAddress, decimals)

        zrxKit = ZrxKit.getInstance(relayers, privateKey, gasInfoProvider, infuraKey)

        wethContract = zrxKit.getWethWrapperInstance(wethAddress)
        zrxExchangeContract = zrxKit.getExchangeInstance(exchangeAddress)

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

        zrxAdapter.transactionsFlowable.subscribe {
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
        zrxBalance.postValue(zrxAdapter.balance)
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
        zrxAdapter.transactions()
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
        val coinWrapper = zrxKit.getErcProxyInstance(address)

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
            2 -> zrxAdapter.transactions()
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
            makerAddress = receiveAddress.toLowerCase(),
            exchangeAddress = exchangeAddress,
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
            takerFee = "0",
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
                    Log.d(TAG, "Order create error $it")
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