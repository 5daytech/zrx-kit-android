package com.blocksdecoded.zrxkit.contracts

import com.blocksdecoded.zrxkit.Constants.ETH_DECIMALS
import io.reactivex.Flowable
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Contract
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigDecimal
import java.math.BigInteger

class WethWrapper(
    contractAddress: String,
    credentials: Credentials,
    contractGasProvider: ContractGasProvider,
    providerUrl: String
) : Contract(BINARY, contractAddress, Web3j.build(HttpService(providerUrl)), credentials, contractGasProvider) {

    val depositEstimatedPrice: BigDecimal
        get() = (depositGasLimit * gasProvider.getGasPrice(FUNC_DEPOSIT))
            .toBigDecimal()
            .movePointLeft(ETH_DECIMALS)
            .stripTrailingZeros()

    val withdrawEstimatedPrice: BigDecimal
        get() = (withdrawGasLimit * gasProvider.getGasPrice(FUNC_WITHDRAW))
            .toBigDecimal()
            .movePointLeft(ETH_DECIMALS)
            .stripTrailingZeros()

    val depositGasLimit: BigInteger
        get() = gasProvider.getGasLimit(FUNC_DEPOSIT)

    val withdrawGasLimit: BigInteger
        get() = gasProvider.getGasLimit(FUNC_WITHDRAW)

    val totalSupply: Flowable<BigInteger>
        get() {
            val function = Function(
                FUNC_TOTAL_SUPPLY,
                listOf(),
                listOf(object : TypeReference<Uint256>() {})
            )
            return executeRemoteCallSingleValueReturn(function, BigInteger::class.java).flowable()
        }

    //region Interface

    fun deposit(amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_DEPOSIT,
            listOf(),
            listOf()
        )
        return executeRemoteCallTransaction(function, amount).flowable()
    }

    fun withdraw(amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_WITHDRAW,
            listOf(Uint256(amount)),
            listOf()
        )
        return executeRemoteCallTransaction(function).flowable()
    }

    fun transfer(toAddress: String, amount: BigInteger): Flowable<Boolean> {
        val function = Function(
            FUNC_TRANSFER,
            listOf(Address(toAddress), Uint256(amount)),
            listOf(object : TypeReference<Bool>() {})
        )
        return executeRemoteCallSingleValueReturn(function, Boolean::class.java).flowable()
    }

    fun approve(spenderAddress: String, amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_APPROVE,
            listOf(Address(spenderAddress), Uint256(amount)),
            listOf()
        )
        return executeRemoteCallTransaction(function).flowable()
    }

    fun transferFrom(fromAddress: String, toAddress: String, amount: BigInteger): Flowable<Boolean> {
        val function = Function(
            FUNC_TRANSFER_FROM,
            listOf(Address(fromAddress), Address(toAddress), Uint256(amount)),
            listOf(object : TypeReference<Bool>() {})
        )
        return executeRemoteCallSingleValueReturn(function, Boolean::class.java).flowable()
    }

    //endregion

    companion object {
        internal const val FUNC_DEPOSIT = "deposit"
        internal const val FUNC_WITHDRAW = "withdraw"
        internal const val FUNC_TOTAL_SUPPLY = "totalSupply"
        internal const val FUNC_TRANSFER = "transfer"
        internal const val FUNC_APPROVE = "approve"
        internal const val FUNC_TRANSFER_FROM = "transferFrom"

        private const val BINARY = ""
    }
}