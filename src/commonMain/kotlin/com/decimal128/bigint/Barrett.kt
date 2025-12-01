package com.decimal128.bigint

class Barrett private constructor (val m: BigInt,
                                   val mBitLen: Int,
                                   val mu: IntArray,
                                   val limbShiftRight: Int,
) {
    val bitShiftRight = mBitLen - 1
    val mLimbLen = (mBitLen + 0x1F) ushr 5
    val q1 = IntArray(mLimbLen)
    val q2 = IntArray(2 * mLimbLen)

    companion object {
        operator fun invoke(m: BigInt): Barrett {
            if (m.isNegative() || m <= 1)
                throw ArithmeticException("Barrett divisor must be >1")
            val bitLen = m.magnitudeBitLen()
            val limbLen = Magia.limbLenFromBitLen(bitLen)
            val unalignedShiftRight = bitLen + 1
            val pad32 = (32 - (unalignedShiftRight % 32)) % 32
            check ((unalignedShiftRight + pad32) % 32 == 0)
            val limbShiftRight = (unalignedShiftRight + pad32) / 32
            val mu = calcMuAligned(m, bitLen, pad32)
            check (mu.size <= 2 * limbLen)
            return Barrett(m, bitLen, mu, limbShiftRight)
        }

        private fun calcMuAligned(m: BigInt, bitLen: Int, pad32: Int): IntArray {
            val x = Magia.newWithSetBit(2 * bitLen)
            val mu = Magia.newDiv(x, m.magia)
            // newShiftLeft works fine for pad == 0
            // and always returns a normalized magia
            val muAligned = Magia.newShiftLeft(mu, pad32)
            return muAligned
        }
    }

    fun remainder(x: BigInt): BigInt {
        val xLen = Magia.bitLen(x.magia)

        // q1 = floor(x / 2^(k - 1))
        val q1 = Magia.newShiftRight(x.magia, bitShiftRight)

        // q2 = q1 * mu
        val q2 = Magia.newMul(q1, mu)

        // q3 = floor(q2 / 2^(k + 1))
        val q3 = Magia.newShiftRight(q2, limbShiftRight * 32)

        // r = x - q3 * m
        val p = Magia.newMul(q3, m.magia)
        check(Magia.compare(x.magia, p) >= 0)
        val r = Magia.newSub(x.magia, p)
        var rLen = Magia.nonZeroLimbLen(r)

        // final correction
        //while (r >= m) r -= m
        // if ... one step is enough
        if (Magia.compare(r, rLen, m.magia, m.magia.size) >= 0) {
            Magia.mutateSub(r, rLen, m.magia, m.magia.size)
            rLen = Magia.nonZeroLimbLen(r, rLen)
        }
        check (Magia.compare(r, rLen, m.magia, m.magia.size) < 0)

        if (rLen == 0)
            return BigInt.ZERO
        return BigInt.fromLittleEndianIntArray(false, r, rLen)
    }


}