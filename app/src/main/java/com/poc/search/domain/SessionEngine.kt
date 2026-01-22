package com.poc.search.domain

import com.poc.search.data.api.DogfaceApiClient
import com.poc.search.data.file.DocumentTreeScanner
import com.poc.search.model.PhotoItem
import com.poc.search.model.Progress
import com.poc.search.model.SessionConfig
import com.poc.search.model.UNKNOWN_KEY

class SessionEngine(
    private val scanner: DocumentTreeScanner,
    private val api: DogfaceApiClient,
    private val prototypeBuilder: PrototypeBuilder,
    private val classifier: Classifier
) {
    suspend fun run(
        cfg: SessionConfig,
        onProgress: (Progress) -> Unit
    ): Map<String, List<PhotoItem>> {

        // 1) Scan incoming
        val incomingUris = scanner.listImagesRecursively(cfg.incomingRoot) { found ->
            onProgress(Progress.Scanning(found))
        }

        // 2) Load reference folders
        val refs = scanner.loadDogRefs(cfg.referenceRoot)

        // 3) Build prototypes
        val protos = prototypeBuilder.build(refs, cfg, onProgress)

        // 4) Embed incoming in batches + classify
        val grouped = linkedMapOf<String, MutableList<PhotoItem>>()
        grouped[UNKNOWN_KEY] = mutableListOf()

        val total = incomingUris.size
        var done = 0

        for (chunk in incomingUris.chunked(cfg.batchSize)) {
            onProgress(Progress.EmbeddingIncoming(done, total))

            val res = api.embedBatch(
                uris = chunk,
                format = cfg.responseFormat,
                maxSidePx = cfg.maxSidePx,
                jpegQuality = cfg.jpegQuality
            )
            val batch = res.embeddings
            val d = batch.d

            for (i in 0 until batch.n) {
                val off = i * d
                val assignment = classifier.classifyRow(
                    rowMajor = batch.vectors,
                    rowOffset = off,
                    protos = protos,
                    topK = cfg.topK,
                    threshold = cfg.unknownThreshold
                )

                val key = assignment.bestDogId ?: UNKNOWN_KEY
                val list = grouped.getOrPut(key) { mutableListOf() }
                list.add(PhotoItem(uri = chunk[i], assignment = assignment))

                done++
                if (done % 50 == 0) onProgress(Progress.Classifying(done, total))
            }
        }

        onProgress(Progress.Done)

        // immutable return
        return grouped.mapValues { it.value.toList() }
    }
}
