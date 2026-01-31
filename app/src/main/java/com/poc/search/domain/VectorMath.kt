package com.poc.search.domain


import kotlin.math.sqrt

object VectorMath {
    fun l2Normalize(v: FloatArray) {
        var s = 0.0
        for (x in v) s += (x * x).toDouble()
        val inv = (1.0 / sqrt(s + 1e-12)).toFloat()
        for (i in v.indices) v[i] *= inv
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot // 이미 L2 normalize 되어 있다고 가정
    }

    fun meanOfRows(rowMajor: FloatArray, n: Int, d: Int): FloatArray {
        val out = FloatArray(d)
        for (i in 0 until n) {
            val off = i * d
            for (j in 0 until d) out[j] += rowMajor[off + j]
        }
        for (j in 0 until d) out[j] /= n.toFloat()
        return out
    }

    fun dotRow(rowMajor: FloatArray, rowOffset: Int, proto: FloatArray): Float {
        var s = 0f
        for (i in proto.indices) s += rowMajor[rowOffset + i] * proto[i]
        return s
    }
}
