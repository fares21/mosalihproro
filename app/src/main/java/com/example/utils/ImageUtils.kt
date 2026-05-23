package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageUtils {
    /**
     * Compress an image from a URI and save it to a local private file.
     * Scales the image to a maximum dimension of 1024px dynamically and avoids OOM.
     */
    fun compressAndSaveImage(context: Context, sourceUri: Uri, prefix: String): String? {
        return try {
            // 1. Decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(sourceUri).use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight
            if (originalWidth <= 0 || originalHeight <= 0) return null

            // Target max dimension of 1024
            val maxDimension = 1024
            var inSampleSize = 1
            if (originalWidth > maxDimension || originalHeight > maxDimension) {
                val halfWidth = originalWidth / 2
                val halfHeight = originalHeight / 2
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode the actual bitmap scaled down by inSampleSize to prevent OutOfMemoryError
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val originalBitmap = context.contentResolver.openInputStream(sourceUri).use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            // 3. Scale precisely to maxDimension if sample size decoding wasn't exact
            val currentWidth = originalBitmap.width
            val currentHeight = originalBitmap.height
            val (newWidth, newHeight) = if (currentWidth > maxDimension || currentHeight > maxDimension) {
                if (currentWidth > currentHeight) {
                    val ratio = maxDimension.toFloat() / currentWidth
                    Pair(maxDimension, (currentHeight * ratio).toInt())
                } else {
                    val ratio = maxDimension.toFloat() / currentHeight
                    Pair((currentWidth * ratio).toInt(), maxDimension)
                }
            } else {
                Pair(currentWidth, currentHeight)
            }

            val finalBitmap = if (newWidth != currentWidth || newHeight != currentHeight) {
                val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                if (scaled != originalBitmap) {
                    originalBitmap.recycle()
                }
                scaled
            } else {
                originalBitmap
            }

            // Create private output file
            val outputDir = File(context.filesDir, "photos").apply { mkdirs() }
            val outputFile = File(outputDir, "${prefix}_${System.currentTimeMillis()}.jpg")
            
            FileOutputStream(outputFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
            
            finalBitmap.recycle()
            
            outputFile.absolutePath
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }

    /**
     * Save signature drawing bitmap in private storage.
     */
    fun saveSignature(context: Context, signatureBitmap: Bitmap): String? {
        return try {
            val outputDir = File(context.filesDir, "signatures").apply { mkdirs() }
            val outputFile = File(outputDir, "sig_${System.currentTimeMillis()}.png")
            FileOutputStream(outputFile).use { out ->
                signatureBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            outputFile.absolutePath
        } catch (t: Throwable) {
            t.printStackTrace()
            null
        }
    }
}
