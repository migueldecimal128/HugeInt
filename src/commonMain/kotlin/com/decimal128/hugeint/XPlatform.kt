@file:Suppress("NOTHING_TO_INLINE")

package com.decimal128.hugeint


expect inline fun unsignedMulHi(x: ULong, y: ULong): ULong

inline fun unsignedMulHi(x: Long, y: Long): Long =
    unsignedMulHi(x.toULong(), y.toULong()).toLong()
