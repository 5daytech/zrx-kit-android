package com.blocksdecoded.zrxkit.contracts

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
import java.math.BigInteger

class WethWrapper(
    contractAddress: String,
    credentials: Credentials,
    contractGasProvider: ContractGasProvider,
    providerUrl: String
) : Contract(BINARY, contractAddress, Web3j.build(HttpService(providerUrl)), credentials, contractGasProvider) {

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
        private const val FUNC_DEPOSIT = "deposit"
        private const val FUNC_WITHDRAW = "withdraw"
        private const val FUNC_TOTAL_SUPPLY = "totalSupply"
        private const val FUNC_TRANSFER = "transfer"
        private const val FUNC_APPROVE = "approve"
        private const val FUNC_TRANSFER_FROM = "transferFrom"

        private const val BINARY = ""
    }
}