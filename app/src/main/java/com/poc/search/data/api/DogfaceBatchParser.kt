package com.poc.search.data.api

import android.os.Build
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class ParsedBatchEmbeddings(
    val n: Int,
    val d: Int,
    val dtypeCode: Int,     // 1=float32, 2=float16
    val vectors: FloatArray // length = n*d, row-major
) {
    fun rowOffset(i: Int): Int = i * d
}

object DogfaceBatchParser {
    private const val HEADER_BYTES = 4 + 4 + 1 // N + D + dtype_code

    fun parseDogfaceBatchV1(payload: ByteArray): ParsedBatchEmbeddings {
        require(payload.size >= HEADER_BYTES) { "Payload too small: ${payload.size}" }

        val buf = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN)
        val n = buf.int
        val d = buf.int
        val dtypeCode = buf.get().toInt() and 0xFF

        require(n > 0 && d > 0) { "Invalid header: n=$n d=$d" }
        require(dtypeCode == 1 || dtypeCode == 2) { "Invalid dtypeCode=$dtypeCode" }

        val bytesPer = if (dtypeCode == 1) 4 else 2
        val expected = HEADER_BYTES + n * d * bytesPer
        require(payload.size == expected) {
            "Invalid payload length. expected=$expected actual=${payload.size} (n=$n d=$d bytesPer=$bytesPer)"
        }

        val out = FloatArray(n * d)

        if (dtypeCode == 1) {
            val dataBuf = ByteBuffer.wrap(payload, HEADER_BYTES, payload.size - HEADER_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .asFloatBuffer()
            dataBuf.get(out)
        } else {
            buf.position(HEADER_BYTES)
            for (i in 0 until n * d) {
                val h = buf.short
                out[i] = halfToFloat(h)
            }
        }

        return ParsedBatchEmbeddings(n = n, d = d, dtypeCode = dtypeCode, vectors = out)
    }

    private fun halfToFloat(h: Short): Float {
        return if (Build.VERSION.SDK_INT >= 26) {
            android.util.Half.toFloat(h)
        } else {
            halfToFloatFallback(h)
        }
    }

    // IEEE-754 half -> float fallback
    private fun halfToFloatFallback(h: Short): Float {
        val bits = h.toInt() and 0xFFFF
        val sign = bits and 0x8000
        val exp = bits and 0x7C00
        val mant = bits and 0x03FF

        val fSign = sign shl 16

        return when (exp) {
            0x0000 -> {
                if (mant == 0) {
                    Float.fromBits(fSign)
                } else {
                    var m = mant
                    var e = -1
                    while ((m and 0x0400) == 0) {
                        m = m shl 1
                        e--
                    }
                    m = m and 0x03FF
                    val expF = (127 - 15 + 1 + e) shl 23
                    val mantF = m shl 13
                    Float.fromBits(fSign or expF or mantF)
                }
            }
            0x7C00 -> {
                val expF = 0xFF shl 23
                val mantF = mant shl 13
                Float.fromBits(fSign or expF or mantF)
            }
            else -> {
                val e = ((exp ushr 10) - 15 + 127) shl 23
                val mantF = mant shl 13
                Float.fromBits(fSign or e or mantF)
            }
        }
    }
}
