package com.poc.search.data.file


import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.poc.search.model.PhotoItem
import com.poc.search.model.UNKNOWN_KEY

class Exporter(private val context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    fun exportGroupedToFolders(
        outputRootUri: Uri,
        grouped: Map<String, List<PhotoItem>>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val root = DocumentFile.fromTreeUri(context, outputRootUri)
            ?: error("Invalid outputRootUri: $outputRootUri")

        val total = grouped.values.sumOf { it.size }
        var done = 0

        grouped.forEach { (key, photos) ->
            val folderName = if (key == UNKNOWN_KEY) "unknown" else key
            val folder = getOrCreateDir(root, folderName)

            photos.forEachIndexed { idx, item ->
                val outName = "img_%05d.jpg".format(idx)
                val outFile = folder.createFile("image/jpeg", outName)
                    ?: error("Failed to create file: $folderName/$outName")

                resolver.openInputStream(item.uri).use { input ->
                    resolver.openOutputStream(outFile.uri).use { output ->
                        requireNotNull(input) { "Cannot open input: ${item.uri}" }
                        requireNotNull(output) { "Cannot open output: ${outFile.uri}" }
                        input.copyTo(output)
                    }
                }

                done++
                if (done % 25 == 0 || done == total) onProgress(done, total)
            }
        }
    }

    private fun getOrCreateDir(parent: DocumentFile, name: String): DocumentFile {
        val existed = parent.findFile(name)
        if (existed != null && existed.isDirectory) return existed
        return parent.createDirectory(name) ?: error("Cannot create directory: $name")
    }
}
