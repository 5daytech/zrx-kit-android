package com.fridaytech.zrxkit.contracts

import com.fridaytech.zrxkit.utils.toEther
import io.reactivex.Flowable
import java.math.BigDecimal
import java.math.BigInteger
import org.web3j.abi.datatypes.Function
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Contract
import org.web3j.tx.gas.ContractGasProvider

internal class WethWrapper(
    contractAddress: String,
    credentials: Credentials,
    contractGasProvider: ContractGasProvider,
    providerUrl: String
) : Contract(BINARY, contractAddress, Web3j.build(HttpService(providerUrl)), credentials, contractGasProvider),
    IWethWrapper {

    override val depositEstimatedPrice: BigDecimal
        get() = (depositGasLimit * gasProvider.getGasPrice(FUNC_DEPOSIT)).toEther()

    override val withdrawEstimatedPrice: BigDecimal
        get() = (withdrawGasLimit * gasProvider.getGasPrice(FUNC_WITHDRAW)).toEther()

    override val depositGasLimit: BigInteger
        get() = gasProvider.getGasLimit(FUNC_DEPOSIT)

    override val withdrawGasLimit: BigInteger
        get() = gasProvider.getGasLimit(FUNC_WITHDRAW)

    //region Interface

    override fun deposit(amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_DEPOSIT,
            listOf(),
            listOf()
        )
        return executeRemoteCallTransaction(function, amount).flowable()
    }

    override fun withdraw(amount: BigInteger): Flowable<TransactionReceipt> {
        val function = Function(
            FUNC_WITHDRAW,
            listOf(Uint256(amount)),
            listOf()
        )
        return executeRemoteCallTransaction(function).flowable()
    }

    //endregion

    companion object {
        internal const val FUNC_DEPOSIT = "deposit"
        internal const val FUNC_WITHDRAW = "withdraw"

        private const val BINARY = ""
    }
}
