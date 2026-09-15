package com.klarfinance.app.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.InputStream

private const val DEFAULT_MAX_UPLOAD_BYTES = 10L * 1024 * 1024

// Generous enough for KTP/selfie legibility, small enough that a typical 12-48MP phone
// photo doesn't need OOM-risking full-resolution decoding just to get thrown away by
// the quality/scale loop below anyway.
private const val MAX_DECODE_DIMENSION_PX = 2048
private const val MIN_JPEG_QUALITY = 40
private const val QUALITY_STEP = 10
private const val MIN_SCALE_DIMENSION_PX = 512

/**
 * Decodes the image at [uri], corrects EXIF rotation (otherwise lost once re-encoded
 * through [Bitmap]), and re-compresses it as JPEG until it fits under [maxBytes] -
 * matching the backend's `spring.servlet.multipart.max-file-size`. Falls back to
 * shrinking dimensions if quality reduction alone can't get there (very large source
 * photos, e.g. picked from gallery rather than captured through the in-app camera).
 */
fun compressImageForUpload(
    context: Context,
    uri: Uri,
    maxBytes: Long = DEFAULT_MAX_UPLOAD_BYTES,
): ByteArray {
    val resolver = context.contentResolver

    // BitmapFactory.decodeStream() with inJustDecodeBounds=true ALWAYS returns null by
    // design (Android contract) - it only populates bounds.outWidth/outHeight. The null
    // check here has to be on openInputStream()'s result, not on decodeStream()'s return
    // value, or this throws unconditionally even for perfectly readable images.
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: throw IllegalStateException("Unable to read image")
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

    val decodeOptions = BitmapFactory.Options().apply {
        inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DECODE_DIMENSION_PX)
    }
    val decodeStream = resolver.openInputStream(uri) ?: throw IllegalStateException("Unable to read image")
    var bitmap = decodeStream.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        ?: throw IllegalStateException("Unable to decode image")

    val rotationDegrees = resolver.openInputStream(uri)?.use(::readExifRotationDegrees) ?: 0
    if (rotationDegrees != 0) {
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    var bytes = compressUnderLimit(bitmap, maxBytes)
    while (bytes.size > maxBytes && bitmap.width > MIN_SCALE_DIMENSION_PX && bitmap.height > MIN_SCALE_DIMENSION_PX) {
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * 0.75f).toInt(),
            (bitmap.height * 0.75f).toInt(),
            true,
        )
        bytes = compressUnderLimit(bitmap, maxBytes)
    }

    return bytes
}

private fun compressUnderLimit(bitmap: Bitmap, maxBytes: Long): ByteArray {
    var quality = 90
    var bytes = encodeJpeg(bitmap, quality)
    while (bytes.size > maxBytes && quality > MIN_JPEG_QUALITY) {
        quality -= QUALITY_STEP
        bytes = encodeJpeg(bitmap, quality)
    }
    return bytes
}

private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray =
    ByteArrayOutputStream().use { stream ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        stream.toByteArray()
    }

private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
    var sampleSize = 1
    val longestSide = maxOf(width, height)
    while (longestSide / (sampleSize * 2) >= maxDimension) {
        sampleSize *= 2
    }
    return sampleSize
}

private fun readExifRotationDegrees(inputStream: InputStream): Int =
    when (ExifInterface(inputStream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90
        ExifInterface.ORIENTATION_ROTATE_180 -> 180
        ExifInterface.ORIENTATION_ROTATE_270 -> 270
        else -> 0
    }
