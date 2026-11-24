package com.mybird.game.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Loads, downsamples, EXIF-rotates, compresses, crops, and caches custom user photos
 * (both circular bird avatar and horizontally-scrollable background) chosen via
 * Android's Photo Picker (`PickVisualMedia`) / Storage Access Framework (SAF).
 *
 * All decoding and bitmap processing executes on [Dispatchers.IO] with strict dimension
 * limits to keep RAM usage minimal and prevent OutOfMemoryError (OOM).
 */
class CustomMediaManager(
    context: Context,
    private val preferences: GamePreferences
) {
    private val appContext: Context = context.applicationContext

    private val cachedBirdFile: File
        get() = File(appContext.filesDir, BIRD_CACHE_FILE)

    private val cachedBgFile: File
        get() = File(appContext.filesDir, BG_CACHE_FILE)

    /**
     * Imports a user-chosen bird photo, takes persistable URI permission when supported,
     * crops into a circular 256x256 sprite with a crisp outline, caches it locally,
     * and persists the URI in [GamePreferences].
     */
    suspend fun importBirdPhoto(uri: Uri): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        runCatching {
            tryTakePersistablePermission(uri)
            val circular = decodeCircularBirdFromUri(uri)
                ?: error("Gagal membaca foto burung.")

            FileOutputStream(cachedBirdFile).use { out ->
                circular.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            preferences.setCustomBirdPhotoUri(uri.toString())
            circular.asImageBitmap()
        }
    }

    /**
     * Imports a user-chosen background photo, takes persistable URI permission when supported,
     * downsamples & compresses it for smooth horizontal scrolling without lag or OOM,
     * caches it locally, and persists the URI in [GamePreferences].
     */
    suspend fun importBackgroundPhoto(uri: Uri): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        runCatching {
            tryTakePersistablePermission(uri)
            val background = decodeBackgroundFromUri(uri)
                ?: error("Gagal membaca foto background.")

            FileOutputStream(cachedBgFile).use { out ->
                background.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            preferences.setCustomBackgroundUri(uri.toString())
            background.asImageBitmap()
        }
    }

    suspend fun loadSavedBirdAvatar(): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBirdFile.exists()) {
                val cached = BitmapFactory.decodeFile(cachedBirdFile.absolutePath)
                if (cached != null) return@runCatching cached.asImageBitmap()
            }
            val savedUri = preferences.getCustomBirdPhotoUri() ?: return@runCatching null
            val decoded = decodeCircularBirdFromUri(Uri.parse(savedUri)) ?: return@runCatching null
            FileOutputStream(cachedBirdFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.PNG, 92, out)
            }
            decoded.asImageBitmap()
        }.getOrNull()
    }

    suspend fun loadSavedBackground(): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBgFile.exists()) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                val cached = BitmapFactory.decodeFile(cachedBgFile.absolutePath, options)
                if (cached != null) return@runCatching cached.asImageBitmap()
            }
            val savedUri = preferences.getCustomBackgroundUri() ?: return@runCatching null
            val decoded = decodeBackgroundFromUri(Uri.parse(savedUri)) ?: return@runCatching null
            FileOutputStream(cachedBgFile).use { out ->
                decoded.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            decoded.asImageBitmap()
        }.getOrNull()
    }

    suspend fun clearBirdPhoto() = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBirdFile.exists()) cachedBirdFile.delete()
            preferences.setCustomBirdPhotoUri(null)
        }
    }

    suspend fun clearBackgroundPhoto() = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBgFile.exists()) cachedBgFile.delete()
            preferences.setCustomBackgroundUri(null)
        }
    }

    private fun tryTakePersistablePermission(uri: Uri) {
        runCatching {
            appContext.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun decodeCircularBirdFromUri(uri: Uri): Bitmap? {
        val raw = decodeDownsampledBitmap(
            uri = uri,
            reqMaxSide = BIRD_AVATAR_PX * 2,
            config = Bitmap.Config.ARGB_8888
        ) ?: return null

        val upright = applyExifOrientation(raw, readExifOrientation(uri))
        if (upright !== raw) raw.recycle()

        val circular = createCircularSprite(upright, BIRD_AVATAR_PX)
        upright.recycle()
        return circular
    }

    private fun decodeBackgroundFromUri(uri: Uri): Bitmap? {
        val raw = decodeDownsampledBitmap(
            uri = uri,
            reqMaxSide = BG_MAX_WIDTH_PX,
            config = Bitmap.Config.RGB_565
        ) ?: return null

        val upright = applyExifOrientation(raw, readExifOrientation(uri))
        if (upright !== raw) raw.recycle()

        // Scale down to memory-safe scrolling dimensions while preserving crisp quality
        val targetHeight = min(upright.height, BG_MAX_HEIGHT_PX).coerceAtLeast(360)
        val rawAspect = upright.width.toFloat() / upright.height.toFloat()
        val targetWidth = (targetHeight * rawAspect).roundToInt()
            .coerceIn(targetHeight, BG_MAX_WIDTH_PX)

        // Center-crop if image is extremely tall or ultra-wide, then resize cleanly
        val desiredAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val cropW: Int
        val cropH: Int
        if (rawAspect > desiredAspect) {
            cropH = upright.height
            cropW = (upright.height * desiredAspect).roundToInt().coerceAtMost(upright.width)
        } else {
            cropW = upright.width
            cropH = (upright.width / desiredAspect).roundToInt().coerceAtMost(upright.height)
        }
        val cropX = ((upright.width - cropW) / 2).coerceAtLeast(0)
        val cropY = ((upright.height - cropH) / 2).coerceAtLeast(0)

        val cropped = Bitmap.createBitmap(upright, cropX, cropY, cropW, cropH)
        if (cropped !== upright) upright.recycle()

        val scaled = Bitmap.createScaledBitmap(cropped, targetWidth, targetHeight, true)
        if (scaled !== cropped) cropped.recycle()
        return scaled
    }

    private fun decodeDownsampledBitmap(
        uri: Uri,
        reqMaxSide: Int,
        config: Bitmap.Config
    ): Bitmap? {
        val resolver = appContext.contentResolver
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        } ?: return null

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                reqMaxSide
            )
            inPreferredConfig = config
        }
        return resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        }
    }

    private fun readExifOrientation(uri: Uri): Int = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }

            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }

            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun createCircularSprite(source: Bitmap, sizePx: Int): Bitmap {
        val minDim = min(source.width, source.height)
        val cropX = (source.width - minDim) / 2
        val cropY = (source.height - minDim) / 2
        val squared = Bitmap.createBitmap(source, cropX, cropY, minDim, minDim)
        val scaled = Bitmap.createScaledBitmap(squared, sizePx, sizePx, true)
        if (squared !== source) squared.recycle()

        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = sizePx * 0.5f
        val borderWidth = sizePx * 0.055f
        val photoRadius = center - borderWidth

        val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(center, center, photoRadius, shaderPaint)

        // Bright white inner ring for pop
        val whiteRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth * 0.85f
            color = 0xFFFFFFFF.toInt()
        }
        canvas.drawCircle(center, center, center - borderWidth * 0.65f, whiteRingPaint)

        // Thin dark cartoon outline
        val outerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = borderWidth * 0.35f
            color = 0xFF3A2714.toInt()
        }
        canvas.drawCircle(center, center, center - borderWidth * 0.2f, outerStrokePaint)

        scaled.recycle()
        return output
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqMaxSide: Int): Int {
        var inSampleSize = 1
        val maxSide = max(width, height)
        while ((maxSide / inSampleSize) > reqMaxSide * 2) {
            inSampleSize *= 2
        }
        return inSampleSize
    }

    companion object {
        private const val BIRD_AVATAR_PX = 256
        private const val BG_MAX_WIDTH_PX = 1440
        private const val BG_MAX_HEIGHT_PX = 960
        private const val BIRD_CACHE_FILE = "custom_bird_avatar.png"
        private const val BG_CACHE_FILE = "custom_background.jpg"
    }
}
