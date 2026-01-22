package com.poc.search.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.poc.search.model.DogRef

class DocumentTreeScanner(private val context: Context) {

    fun listImagesRecursively(rootUri: Uri, onFound: (Int) -> Unit = {}): List<Uri> {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return emptyList()
        val out = ArrayList<Uri>(2048)
        var found = 0

        fun walk(dir: DocumentFile) {
            dir.listFiles().forEach { f ->
                if (f.isDirectory) {
                    walk(f)
                } else if (isImage(f)) {
                    out.add(f.uri)
                    found++
                    if (found % 50 == 0) onFound(found)
                }
            }
        }

        walk(root)
        onFound(found)
        return out
    }

    /**
     * referenceRoot/
     *   dog_001/ ref1.jpg ref2.jpg ...
     *   dog_002/ ...
     */
    fun loadDogRefs(referenceRoot: Uri): List<DogRef> {
        val root = DocumentFile.fromTreeUri(context, referenceRoot) ?: return emptyList()

        return root.listFiles()
            .filter { it.isDirectory }
            .mapNotNull { folder ->
                val dogId = folder.name ?: return@mapNotNull null
                val refs = folder.listFiles()
                    .filter { !it.isDirectory && isImage(it) }
                    .map { it.uri }
                if (refs.isEmpty()) null else DogRef(dogId, refs)
            }
            .sortedBy { it.dogId }
    }

    private fun isImage(f: DocumentFile): Boolean {
        val mime = f.type
        if (mime != null && mime.startsWith("image/")) return true
        val name = f.name?.lowercase() ?: return false
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".webp")
    }
}
