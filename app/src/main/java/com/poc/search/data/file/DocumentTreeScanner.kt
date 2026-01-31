package com.poc.search.data.file

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.poc.search.model.DogRef
import java.io.File

class DocumentTreeScanner(private val context: Context) {

    fun listImagesRecursively(rootUri: Uri, onFound: (Int) -> Unit = {}): List<Uri> {
        // Uri가 file:// 로 시작하면 일반 File API 사용
        if (rootUri.scheme == "file") {
            val rootFile = File(rootUri.path ?: return emptyList())
            val out = mutableListOf<Uri>()
            rootFile.walkTopDown().filter { it.isFile && isImageFile(it) }.forEach {
                out.add(Uri.fromFile(it))
                if (out.size % 50 == 0) onFound(out.size)
            }
            onFound(out.size)
            return out
        }

        // 기존 SAF 방식 유지
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

    fun loadDogRefs(referenceRoot: Uri): List<DogRef> {
        if (referenceRoot.scheme == "file") {
            val rootFile = File(referenceRoot.path ?: return emptyList())
            return rootFile.listFiles { it -> it.isDirectory }?.mapNotNull { folder ->
                val dogId = folder.name
                val refs = folder.listFiles { f -> f.isFile && isImageFile(f) }?.map { Uri.fromFile(it) } ?: emptyList()
                if (refs.isEmpty()) null else DogRef(dogId, refs)
            }?.sortedBy { it.dogId } ?: emptyList()
        }

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
        return isImageName(f.name ?: "")
    }

    private fun isImageFile(f: File): Boolean = isImageName(f.name)

    private fun isImageName(name: String): Boolean {
        val low = name.lowercase()
        return low.endsWith(".jpg") || low.endsWith(".jpeg") || low.endsWith(".png") || low.endsWith(".webp")
    }
}
