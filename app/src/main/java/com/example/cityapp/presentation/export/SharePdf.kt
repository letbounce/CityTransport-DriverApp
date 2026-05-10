package com.example.cityapp.presentation.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

fun writePdfBytesToCache(context: Context, fileName: String, bytes: ByteArray): File {
    val dir = File(context.cacheDir, "exports").apply { mkdirs() }
    return File(dir, fileName).also { it.writeBytes(bytes) }
}

fun sharePdfFile(context: Context, file: File, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle))
}
