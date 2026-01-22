package com.poc.search.domain


import com.poc.search.data.api.DogfaceApiClient
import com.poc.search.model.DogPrototype
import com.poc.search.model.DogRef
import com.poc.search.model.Progress
import com.poc.search.model.SessionConfig

class PrototypeBuilder(
    private val api: DogfaceApiClient
) {
    suspend fun build(
        refs: List<DogRef>,
        cfg: SessionConfig,
        onProgress: (Progress) -> Unit
    ): List<DogPrototype> {
        val out = ArrayList<DogPrototype>(refs.size)
        val total = refs.size
        var done = 0

        for (dog in refs) {
            // 레퍼런스는 보통 수가 적으니 간단히 전체를 배치로 호출
            val batch = embedAll(dog.refUris, cfg)
            val proto = VectorMath.meanOfRows(batch.vectors, batch.n, batch.d)
            VectorMath.l2Normalize(proto)

            out.add(DogPrototype(dogId = dog.dogId, vector = proto))
            done++
            onProgress(Progress.EmbeddingRef(done, total))
        }

        return out
    }

    private suspend fun embedAll(uris: List<android.net.Uri>, cfg: SessionConfig): com.poc.search.data.api.ParsedBatchEmbeddings {
        val total = uris.size
        val chunks = uris.chunked(cfg.batchSize)

        var dim = -1
        var writePos = 0
        var flat: FloatArray? = null

        for (c in chunks) {
            val res = api.embedBatch(
                uris = c,
                format = cfg.responseFormat,
                maxSidePx = cfg.maxSidePx,
                jpegQuality = cfg.jpegQuality
            )
            val b = res.embeddings
            if (dim == -1) {
                dim = b.d
                flat = FloatArray(total * dim)
            }
            System.arraycopy(b.vectors, 0, flat!!, writePos, b.n * dim)
            writePos += b.n * dim
        }

        return com.poc.search.data.api.ParsedBatchEmbeddings(
            n = total,
            d = dim,
            dtypeCode = 1,
            vectors = flat!!
        )
    }
}
