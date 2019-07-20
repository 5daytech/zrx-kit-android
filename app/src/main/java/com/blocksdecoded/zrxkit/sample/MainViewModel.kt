package com.blocksdecoded.zrxkit.sample

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.blocksdecoded.zrxkit.contracts.WethWrapper
import com.blocksdecoded.zrxkit.sample.core.Erc20Adapter
import com.blocksdecoded.zrxkit.sample.core.EthereumAdapter
import com.blocksdecoded.zrxkit.sample.core.TransactionRecord
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.hdwalletkit.HDWallet
import io.horizontalsystems.hdwalletkit.Mnemonic
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.Credentials
import org.web3j.crypto.ECKeyPair
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigDecimal
import java.math.BigInteger

class MainViewModel: ViewModel() {

    private val infuraCredentials = EthereumKit.InfuraCredentials(
        projectId = "2a1306f1d12f4c109a4d4fb9be46b02e",
        secretKey = "fc479a9290b64a84a15fa6544a130218")
    private val etherscanKey = "GKNHXT22ED7PRVCKZATFZQD1YI7FK9AAYE"
    private val networkType: EthereumKit.NetworkType = EthereumKit.NetworkType.Kovan

    private val tempInfuraUrl = "https://kovan.infura.io/57b6615fb10b4749a54b29c2894a00df"

    private val wethAddress = "0xd0a1e359811322d97991e03f863a0c30c2cf029c"
    private val zrxAddress = "0x2002d3812f58e35f0ea1ffbf80a75a38c32175fa"
    private val contractDecimal = 18

    private val gasPriceProvider = object : ContractGasProvider {
        override fun getGasLimit(contractFunc: String?): BigInteger = 100_000.toBigInteger()
        override fun getGasLimit(): BigInteger = 100_000.toBigInteger()

        // Gas price 5 GWei
        override fun getGasPrice(contractFunc: String?): BigInteger = 5_000_000_000.toBigInteger()
        override fun getGasPrice(): BigInteger = 5_000_000_000.toBigInteger()
    }

    private lateinit var wethContract: WethWrapper
    private lateinit var ethereumKit: EthereumKit
    private lateinit var ethereumAdapter: EthereumAdapter
    private lateinit var wethAdapter: Erc20Adapter
    private lateinit var zrxAdapter: Erc20Adapter

    private val disposables = CompositeDisposable()

    val ethBalance = MutableLiveData<BigDecimal>()
    val wethBalance = MutableLiveData<BigDecimal>()
    val zrxBalance = MutableLiveData<BigDecimal>()
    val transactions = MutableLiveData<List<TransactionRecord>>()
    val lastBlockHeight = MutableLiveData<Long>()

    val alertEvent = SingleLiveEvent<String>()
    val messageEvent = SingleLiveEvent<String>()

    val receiveAddress: String
        get() = ethereumKit.receiveAddress

    init {
        init()
    }

    private fun init() {
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
        wethAdapter = Erc20Adapter(App.instance, ethereumKit, "Wrapped Eth", "WETH", wethAddress, contractDecimal)
        zrxAdapter = Erc20Adapter(App.instance, ethereumKit, "0x", "ZRX", zrxAddress, contractDecimal)

        val credentials = Credentials.create(ECKeyPair.create(privateKey))
        wethContract = WethWrapper(wethAddress, credentials, gasPriceProvider, tempInfuraUrl)

        updateEthBalance()
        updateWethBalance()
        updateErc20Balance()
        updateLastBlockHeight()

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

    //endregion

    private fun showMessage(message: String) {
        alertEvent.postValue(message)
    }

    //region Public

    fun refresh() {
        ethereumKit.refresh()
    }

    fun wrapEther(amount: BigDecimal) {
        messageEvent.postValue("Wrap started")

        val amountInt = amount.movePointRight(contractDecimal).stripTrailingZeros().toBigInteger()

        wethContract.deposit(amountInt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showMessage("$amount Eth wrapped")
                }, {
                    showMessage("Wrap error ${it.message}")
                }).let { disposables.add(it) }
    }

    fun unwrapEther(amount: BigDecimal) {
        messageEvent.postValue("Unwrap started")

        val amountInt = amount.movePointRight(contractDecimal).stripTrailingZeros().toBigInteger()

        wethContract.withdraw(amountInt)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showMessage("$amount Weth unwrapped")
                }, {
                    showMessage("Unwrap error ${it.message}")
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

    //endregion
}