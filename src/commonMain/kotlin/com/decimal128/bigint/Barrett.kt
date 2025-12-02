package com.decimal128.bigint

class Barrett private constructor (val m: BigInt,
                                   val muBits: BigInt,
                                   val muLimbs: BigInt,
) {
    val mSquared = m.sqr()
    val kBits = m.magnitudeBitLen()
    val kLimbs = m.magia.size

    companion object {
        operator fun invoke(m: BigInt): Barrett {
            if (m.isNegative() || m <= 1)
                throw ArithmeticException("Barrett divisor must be >1")
            val mNormalized = m.normalize()
            val muBits = calcMuBits(mNormalized)
            val muLimbs = calcMuLimbs(mNormalized)
            return Barrett(mNormalized, muBits, muLimbs)
        }

        private fun calcMuBits(m: BigInt): BigInt {
            check (m.isNormalized())
            val mBitLen = m.magnitudeBitLen()
            val x = BigInt.withSetBit(2 * mBitLen)
            val mu = x / m
            return mu
        }

        private fun calcMuLimbs(m: BigInt): BigInt {
            check (m.isNormalized())
            val mLimbLen = m.magia.size
            val x = BigInt.withSetBit(2 * mLimbLen * 32)
            val mu = x / m
            return mu
        }
    }

    fun remainder(x: BigInt): BigInt {
        val bitsAnswer = reduceBits(x)
        val limbsAnswer = reduceLimbs(x)
        check (bitsAnswer == limbsAnswer)
        return limbsAnswer
    }

    fun reduceBits(x: BigInt): BigInt {
        require(x >= 0)
        require(x < mSquared)

        if (x < m) return x

        // q = floor(x * μ / 2^(2k))
        val q = (x * muBits) shr (2*kBits)

        // r = x - q*m
        var r = x - q*m

        if (r >= m) r -= m
        return r
    }

    fun reduceLimbs(x: BigInt): BigInt {
        require (x >= 0)
        require (x < mSquared)
        if (x < m) return x

        // q1 = floor(x / b**(k - 1))
        val q1 = x ushr ((kLimbs - 1) * 32)
        // q2 = q1 * mu
        val q2 = q1 * muLimbs
        // q3 = floor(q2 / b**(k + 1))
        val q3 = q2 ushr ((kLimbs + 1) * 32)
        // r1 = x % b**(k + 1)
        val r1 = x and BigInt.withBitMask((kLimbs + 1) * 32)
        // r2 = (q3 * m) % b**(k + 1)
        val r2 = (q3 * m) and BigInt.withBitMask((kLimbs + 1) * 32)
        // r = r1 - r2
        var r = r1 - r2
        if (r.isNegative())
            r = r + BigInt.withSetBit((kLimbs + 1) * 32)
        while (r >= m)
            r = r - m
        return r
    }
}