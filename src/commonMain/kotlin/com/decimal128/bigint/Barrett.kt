package com.decimal128.bigint

class Barrett private constructor (val m: IntArray,
                                   val mBitLen: Int,
                                   val mu: IntArray,
                                   val limbShiftRight: Int,
) {
    val bitShiftRight = mBitLen - 1
    // initial q1 estimate can be > m ... so temp array must be 1 bit bigger
    val tmp1 = IntArray((mBitLen + 1 + 31) shr 5)
    val tmp2 = IntArray(2 * tmp1.size)

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
            return Barrett(m.magia, bitLen, mu, limbShiftRight)
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
        val xLen = Magia.nonZeroLimbLen(x.magia)

        // q1 = floor(x / 2^(k - 1))
        val q1 = tmp1
        val q1Len = Magia.setShiftRight(q1, x.magia, xLen, bitShiftRight)

        // q2 = q1 * mu
        val q2 = tmp2
        q2.fill(0, 0, mu.size)
        val q2Len = Magia.mul(q2, q1, q1Len, mu, mu.size)

        // q3 = floor(q2 / 2^(k + 1))
        val q3 = tmp1
        q2.copyInto(q1, 0, limbShiftRight, limbShiftRight + q2Len)
        val q3Len = q2Len - limbShiftRight

        // p = q3 * m
        val p = tmp2
        p.fill(0, 0, mu.size)
        val pLen = Magia.mul(p, q3, q3Len, m, m.size)
        // r = x - q3 * m
        check(Magia.compare(x.magia, xLen, p, pLen) >= 0)
        val r = tmp1
        var rLen = 0 // Magia.setSub(r, x.magia, xLen, p, pLen)

        // final correction
        //while (r >= m) r -= m
        // if ... one step is enough
        if (Magia.compare(r, rLen, m, m.size) >= 0) {
            Magia.mutateSub(r, rLen, m, m.size)
            rLen = Magia.nonZeroLimbLen(r, rLen)
        }
        check (Magia.compare(r, rLen, m, m.size) < 0)

        if (rLen == 0)
            return BigInt.ZERO
        return BigInt.fromLittleEndianIntArray(false, r, rLen)
    }


}