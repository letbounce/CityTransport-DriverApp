package com.example.cityapp.presentation.incident

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object IncidentPhotoUtils {
    fun bitmapToJpegDataUri(bitmap: Bitmap, quality: Int = 82): String {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        return "data:image/jpeg;base64,$b64"
    }
}
