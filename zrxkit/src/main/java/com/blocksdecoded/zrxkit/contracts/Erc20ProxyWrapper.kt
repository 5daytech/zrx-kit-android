package com.blocksdecoded.zrxkit.contracts

import com.blocksdecoded.zrxkit.Constants.MAX_ALLOWANCE
import io.reactivex.Flowable
import org.web3j.abi.TypeReference
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.Bool
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.Type
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Contract
import org.web3j.tx.gas.ContractGasProvider
import java.math.BigInteger

class Erc20ProxyWrapper(
    contractAddress: String,
    credentials: Credentials,
    contractGasProvider: ContractGasProvider,
    providerUrl: String,
    private var proxyAddress: String = "0xf1ec01d6236d3cd881a0bf0130ea25fe4234003e"
) : Contract(BINARY, contractAddress, Web3j.build(HttpService(providerUrl)), credentials, contractGasProvider) {

    fun lockProxy(): Flowable<TransactionReceipt> = approve(proxyAddress, BigInteger.valueOf(0))

    fun setUnlimitedProxyAllowance(): Flowable<TransactionReceipt> = approve(proxyAddress, MAX_ALLOWANCE)

    fun proxyAllowance(ownerAddress: String): Flowable<BigInteger> = allowance(ownerAddress, proxyAddress)

    fun balanceOf(address: String): Flowable<BigInteger> {
        val function = Function(
            FUNC_BALANCE_OF,
            listOf<Type<*>>(Address(address)),
            listOf<TypeReference<*>>(object : TypeReference<Uint256>() {})
        )
        return executeRemoteCallSingleValueReturn(function, BigInteger::class.java).flowable()
    }

    fun allowance(ownerAddress: String, spenderAddress: String): Flowable<BigInteger> {
        val function = Function(
            FUNC_ALLOWANCE,
            listOf<Type<*>>(Address(ownerAddress), Address(spenderAddress)),
            listOf<TypeReference<*>>(object : TypeReference<Uint256>() {})
        )
        return executeRemoteCallSingleValueReturn(function, BigInteger::class.java).flowable()
    }

    fun transfer(toAddress: String, amount: BigInteger): Flowable<Boolean> {
        val function = Function(
            FUNC_TRANSFER,
            listOf<Type<*>>(Address(toAddress), Uint256(amount)),
            listOf<TypeReference<*>>(object : TypeReference<Bool>() {})
        )
        return executeRemoteCallSingleValueReturn(function, Boolean::class.java).flowable()
    }

    fun approve(spenderAddress: String, amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_APPROVE,
            listOf<Type<*>>(Address(spenderAddress), Uint256(amount)),
            listOf<TypeReference<*>>()
        )
        return executeRemoteCallTransaction(function).flowable()
    }

    fun transferFrom(fromAddress: String, toAddress: String, amount: BigInteger): Flowable<Boolean> {
        val function = Function(
            FUNC_TRANSFER_FROM,
            listOf<Type<*>>(Address(fromAddress), Address(toAddress), Uint256(amount)),
            listOf<TypeReference<*>>(object : TypeReference<Bool>() {})
        )
        return executeRemoteCallSingleValueReturn(function, Boolean::class.java).flowable()
    }

    companion object {
        private const val FUNC_BALANCE_OF = "balanceOf"
        private const val FUNC_ALLOWANCE = "allowance"
        private const val FUNC_TRANSFER = "transfer"
        private const val FUNC_APPROVE = "approve"
        private const val FUNC_TRANSFER_FROM = "transferFrom"

        private const val BINARY = ""
    }
}