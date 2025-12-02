package com.decimal128.bigint

class Barrett private constructor (val m: BigInt,
                                   val k: Int,
                                   val mu: BigInt,
) {
    val mSquared = m.sqr()
    val shiftKminus1 = k - 1
    val shiftKplus1 = k + 1
    val maskKplus1 = BigInt.withBitMask(shiftKplus1)

    companion object {
        operator fun invoke(m: BigInt): Barrett {
            if (m.isNegative() || m <= 1)
                throw ArithmeticException("Barrett divisor must be >1")
            val mBitLen = m.magnitudeBitLen()
            val mu = calcMu(m, mBitLen)
            return Barrett(m, mBitLen, mu)
        }

        private fun calcMu(m: BigInt, mBitLen: Int): BigInt {
            val x = BigInt.withSetBit(2 * mBitLen)
            val mu = x / m
            return mu
        }
        /*
        private fun calcMuAligned(m: BigInt, bitLen: Int, pad32: Int): IntArray {
            val x = Magia.newWithSetBit(2 * bitLen)
            val mu = Magia.newDiv(x, m.magia)
            // newShiftLeft works fine for pad == 0
            // and always returns a normalized magia
            val muAligned = Magia.newShiftLeft(mu, pad32)
            return muAligned
        }

         */
    }

    fun remainder(x: BigInt): BigInt {
        require(x >= 0)
        require(x < mSquared)

        if (x < m) return x

        // q = floor(x * μ / 2^(2k))
        val q = (x * mu) shr (2*k)

        // r = x - q*m
        var r = x - q*m

        if (r >= m) r -= m
        return r
    }
}