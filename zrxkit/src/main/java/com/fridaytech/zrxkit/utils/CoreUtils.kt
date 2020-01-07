package com.fridaytech.zrxkit.utils

import java.math.BigInteger
import org.web3j.tx.gas.ContractGasProvider

internal object CoreUtils {
    fun getProtocolFee(gasInfoProvider: ContractGasProvider, fillOrdersCount: Int): BigInteger {
        return (BigInteger.valueOf((150000L * fillOrdersCount)) * gasInfoProvider.getGasPrice(""))
    }
}
