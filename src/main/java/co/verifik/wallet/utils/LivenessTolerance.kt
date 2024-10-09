package co.verifik.wallet.utils

/**
 * LivenessTolerance is an enum class that holds the different levels of liveness tolerance:
 *
 * - REGULAR: The liveness tolerance is regular
 * - SOFT: The liveness tolerance is soft
 * - HARDENED: The liveness tolerance is hardened
 */
enum class LivenessTolerance {
    REGULAR,
    SOFT,
    HARDENED,
}
