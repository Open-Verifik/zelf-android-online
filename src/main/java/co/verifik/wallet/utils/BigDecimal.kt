package co.verifik.wallet.utils

import java.math.BigDecimal

fun BigDecimal.weiToEth(): BigDecimal {
    return this.divide(BigDecimal("1000000000000000000"))
}

fun BigDecimal.gweiToEth(): BigDecimal {
    return this.divide(BigDecimal("1000000000"))
}