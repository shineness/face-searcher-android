package com.facesearch.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.facesearch.app.FaceSearchApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Image fingerprint utility using pHash (perceptual hash) algorithm
 * More robust than simple pixel comparison
 */
object ImageHasher {
    private const val TAG = "ImageHasher"
    private const val HASH_SIZE = 8 // 8x8 = 64 bits
    
    /**
     * Calculate perceptual hash of an image
     */
    suspend fun calculateHash(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            Logger.logInfo("Calculating hash for: $uri")
            
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            if (originalBitmap == null) {
                Logger.logError("ImageHash", "Failed to decode image: $uri")
                return@withContext null
            }
            
            // Resize to 8x8
            val resized = Bitmap.createScaledBitmap(originalBitmap, HASH_SIZE, HASH_SIZE, true)
            originalBitmap.recycle()
            
            // Convert to grayscale
            val gray = toGrayscale(resized)
            resized.recycle()
            
            // Calculate average value
            val avg = calculateAverage(gray)
            
            // Generate hash based on comparison with average
            val hash = StringBuilder()
            for (y in 0 until HASH_SIZE) {
                for (x in 0 until HASH_SIZE) {
                    val pixel = gray.getPixel(x, y)
                    val r = (pixel shr 16) and 0xFF
                    hash.append(if (r >= avg) "1" else "0")
                }
            }
            
            gray.recycle()
            
            val result = hash.toString()
            Logger.logInfo("Hash calculated: ${result.take(16)}...")
            result
            
        } catch (e: Exception) {
            Logger.logError("ImageHash", "Error calculating hash", e)
            null
        }
    }
    
    /**
     * Calculate similarity between two hashes (Hamming distance)
     * Returns percentage (0-100)
     */
    fun calculateSimilarity(hash1: String, hash2: String): Int {
        if (hash1.length != hash2.length) {
            return 0
        }
        
        var diff = 0
        for (i in hash1.indices) {
            if (hash1[i] != hash2[i]) {
                diff++
            }
        }
        
        val maxDiff = hash1.length
        return ((maxDiff - diff) * 100 / maxDiff)
    }
    
    /**
     * Compare two images and return similarity percentage
     */
    suspend fun compareImages(context: Context, uri1: Uri, uri2: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val hash1 = calculateHash(context, uri1) ?: return@withContext 0
            val hash2 = calculateHash(context, uri2) ?: return@withContext 0
            
            val similarity = calculateSimilarity(hash1, hash2)
            Logger.logInfo("Similarity between images: $similarity%")
            similarity
            
        } catch (e: Exception) {
            Logger.logError("CompareImages", "Error comparing images", e)
            0
        }
    }
    
    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                // Luminosity formula
                val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                
                grayBitmap.setPixel(x, y, (0xFF shl 24) or (gray shl 16) or (gray shl 8) or gray)
            }
        }
        
        return grayBitmap
    }
    
    private fun calculateAverage(bitmap: Bitmap): Int {
        var sum = 0
        val width = bitmap.width
        val height = bitmap.height
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                sum += r
            }
        }
        
        return sum / (width * height)
    }
}
