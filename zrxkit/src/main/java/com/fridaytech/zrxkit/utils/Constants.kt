package com.fridaytech.zrxkit.utils

import java.math.BigInteger

internal object Constants {
    const val ETH_DECIMALS = 18
    const val ETH_ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    val MAX_ALLOWANCE = BigInteger.valueOf(2).pow(256).minus(1.toBigInteger())
}
