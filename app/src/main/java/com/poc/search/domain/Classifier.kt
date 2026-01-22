package com.poc.search.domain


import com.poc.search.model.Assignment
import com.poc.search.model.Candidate
import com.poc.search.model.DogPrototype

class Classifier {
    fun classifyRow(
        rowMajor: FloatArray,
        rowOffset: Int,
        protos: List<DogPrototype>,
        topK: Int,
        threshold: Float
    ): Assignment {
        val scored = protos.map { p ->
            Candidate(p.dogId, VectorMath.dotRow(rowMajor, rowOffset, p.vector))
        }.sortedByDescending { it.score }

        val top = scored.take(topK)
        val best = top.firstOrNull()
        val unknown = best == null || best.score < threshold

        return Assignment(
            bestDogId = if (unknown) null else best!!.dogId,
            bestScore = best?.score ?: -1f,
            top = top
        )
    }
}
