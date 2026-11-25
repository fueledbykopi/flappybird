package com.mybird.game.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * User-adjusted circular framing transform for a custom bird photo.
 *
 * - [zoom] = 1f fits the image's shorter side exactly to the circle diameter.
 * - [panNormX] / [panNormY] are normalized offsets in units of the circle diameter
 *   (positive X moves the image right, positive Y moves the image down).
 */
data class BirdCropTransform(
    val zoom: Float = 1f,
    val panNormX: Float = 0f,
    val panNormY: Float = 0f
)

/**
 * Decoded, upright custom bird image waiting for user pan/zoom/rotate adjustment
 * before being cropped and saved as the active bird avatar.
 */
data class PendingBirdImage(
    val uri: Uri,
    val sourceBitmap: Bitmap,
    val previewBitmap: ImageBitmap
)

/**
 * Loads, downsamples, EXIF-rotates, compresses, crops, and caches custom user photos
 * (both circular bird avatar and horizontally-scrollable background) chosen via
 * Android's Photo Picker (`PickVisualMedia`), Storage Access Framework (`OpenDocument`),
 * or `GetContent`.
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
     * Decodes a user-chosen bird photo into an upright software bitmap ready for
     * interactive pan/zoom/rotate framing before applying it to the game.
     */
    suspend fun loadBirdForCropping(uri: Uri): Result<PendingBirdImage> = withContext(Dispatchers.IO) {
        runCatching {
            tryTakePersistablePermission(uri)
            val decoded = decodeSoftwareBitmapFromUri(uri, reqMaxSide = BIRD_CROP_PREVIEW_MAX_PX)
                ?: error("Format foto tidak didukung atau tidak bisa dibaca.")
            PendingBirdImage(
                uri = uri,
                sourceBitmap = decoded,
                previewBitmap = decoded.asImageBitmap()
            )
        }.onFailure { error ->
            Log.e(TAG, "Failed to load bird photo for cropping from $uri", error)
        }
    }

    /**
     * Rotates a pending bird image 90 degrees clockwise on [Dispatchers.IO].
     */
    suspend fun rotatePendingBird90(pending: PendingBirdImage): Result<PendingBirdImage> =
        withContext(Dispatchers.IO) {
            runCatching {
                val matrix = Matrix().apply { postRotate(90f) }
                val rotated = Bitmap.createBitmap(
                    pending.sourceBitmap,
                    0,
                    0,
                    pending.sourceBitmap.width,
                    pending.sourceBitmap.height,
                    matrix,
                    true
                )
                PendingBirdImage(
                    uri = pending.uri,
                    sourceBitmap = rotated,
                    previewBitmap = rotated.asImageBitmap()
                )
            }
        }

    /**
     * Applies the user's [crop] framing to [pending], renders the circular 256x256
     * bird sprite with its crisp white/dark outline, caches it atomically, and persists
     * the URI in [GamePreferences].
     */
    suspend fun saveCroppedBirdPhoto(
        pending: PendingBirdImage,
        crop: BirdCropTransform
    ): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        runCatching {
            val circular = createCircularSprite(
                source = pending.sourceBitmap,
                sizePx = BIRD_AVATAR_PX,
                crop = crop
            )
            writeBitmapAtomically(circular, cachedBirdFile, Bitmap.CompressFormat.PNG, 95)
            preferences.setCustomBirdPhotoUri(pending.uri.toString())
            circular.asImageBitmap()
        }.onFailure { error ->
            Log.e(TAG, "Failed to save cropped bird photo", error)
        }
    }

    /**
     * Direct import helper using default centered framing (kept for reload fallback).
     */
    suspend fun importBirdPhoto(
        uri: Uri,
        crop: BirdCropTransform = BirdCropTransform()
    ): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        runCatching {
            tryTakePersistablePermission(uri)
            val circular = decodeCircularBirdFromUri(uri, crop)
                ?: error("Format foto tidak didukung atau tidak bisa dibaca.")

            writeBitmapAtomically(circular, cachedBirdFile, Bitmap.CompressFormat.PNG, 95)
            preferences.setCustomBirdPhotoUri(uri.toString())
            circular.asImageBitmap()
        }.onFailure { error ->
            Log.e(TAG, "Failed to import bird photo from $uri", error)
        }
    }

    /**
     * Imports a user-chosen background photo, takes persistable URI permission when supported,
     * preserves its aspect ratio across portrait/landscape/panoramic shapes, composites alpha
     * safely, caches it locally, and persists the URI in [GamePreferences].
     */
    suspend fun importBackgroundPhoto(uri: Uri): Result<ImageBitmap> = withContext(Dispatchers.IO) {
        runCatching {
            tryTakePersistablePermission(uri)
            val background = decodeBackgroundFromUri(uri)
                ?: error("Format foto background tidak didukung atau tidak bisa dibaca.")

            writeBitmapAtomically(background, cachedBgFile, Bitmap.CompressFormat.JPEG, 88)
            preferences.setCustomBackgroundUri(uri.toString())
            background.asImageBitmap()
        }.onFailure { error ->
            Log.e(TAG, "Failed to import background photo from $uri", error)
        }
    }

    suspend fun loadSavedBirdAvatar(): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBirdFile.exists() && cachedBirdFile.length() > 0L) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val cached = BitmapFactory.decodeFile(cachedBirdFile.absolutePath, options)
                if (cached != null) return@runCatching cached.asImageBitmap()
            }
            val savedUri = preferences.getCustomBirdPhotoUri() ?: return@runCatching null
            val decoded = decodeCircularBirdFromUri(Uri.parse(savedUri), BirdCropTransform())
                ?: return@runCatching null
            writeBitmapAtomically(decoded, cachedBirdFile, Bitmap.CompressFormat.PNG, 95)
            decoded.asImageBitmap()
        }.onFailure { error ->
            Log.w(TAG, "Failed to load saved bird avatar", error)
        }.getOrNull()
    }

    suspend fun loadSavedBackground(): ImageBitmap? = withContext(Dispatchers.IO) {
        runCatching {
            if (cachedBgFile.exists() && cachedBgFile.length() > 0L) {
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val cached = BitmapFactory.decodeFile(cachedBgFile.absolutePath, options)
                if (cached != null) return@runCatching cached.asImageBitmap()
            }
            val savedUri = preferences.getCustomBackgroundUri() ?: return@runCatching null
            val decoded = decodeBackgroundFromUri(Uri.parse(savedUri)) ?: return@runCatching null
            writeBitmapAtomically(decoded, cachedBgFile, Bitmap.CompressFormat.JPEG, 88)
            decoded.asImageBitmap()
        }.onFailure { error ->
            Log.w(TAG, "Failed to load saved background", error)
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

    private fun decodeCircularBirdFromUri(uri: Uri, crop: BirdCropTransform): Bitmap? {
        val decoded = decodeSoftwareBitmapFromUri(uri, reqMaxSide = BIRD_CROP_PREVIEW_MAX_PX)
            ?: return null
        return try {
            createCircularSprite(decoded, BIRD_AVATAR_PX, crop)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    private fun decodeBackgroundFromUri(uri: Uri): Bitmap? {
        val decoded = decodeSoftwareBitmapFromUri(uri, reqMaxSide = BG_MAX_WIDTH_PX) ?: return null
        return try {
            createBackgroundBitmap(decoded)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    private fun decodeSoftwareBitmapFromUri(uri: Uri, reqMaxSide: Int): Bitmap? {
        val tempFile = copyUriToTempFile(uri) ?: return null
        try {
            val bitmap = decodeWithImageDecoderIfSupported(tempFile, reqMaxSide)
                ?: decodeWithBitmapFactory(tempFile, reqMaxSide)
                ?: return null

            return ensureSoftwareArgb8888(bitmap)
        } finally {
            runCatching { tempFile.delete() }
        }
    }

    private fun copyUriToTempFile(uri: Uri): File? {
        val inputStream = openUriInputStream(uri) ?: return null
        val tempFile = File.createTempFile("mybird_pick_", ".img", appContext.cacheDir)
        var totalBytes = 0L
        return try {
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        totalBytes += read
                        if (totalBytes > MAX_IMPORT_FILE_BYTES) {
                            throw IOException("Ukuran file gambar terlalu besar (maks. 35 MB).")
                        }
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            }
            if (totalBytes <= 0L) {
                tempFile.delete()
                null
            } else {
                tempFile
            }
        } catch (t: Throwable) {
            runCatching { tempFile.delete() }
            throw t
        }
    }

    private fun openUriInputStream(uri: Uri): InputStream? {
        val resolver = appContext.contentResolver
        runCatching { resolver.openInputStream(uri) }.getOrNull()?.let { return it }
        return runCatching {
            resolver.openAssetFileDescriptor(uri, "r")?.createInputStream()
        }.getOrNull()
    }

    private fun decodeWithImageDecoderIfSupported(file: File, reqMaxSide: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            val source = ImageDecoder.createSource(file)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
                val (targetW, targetH) = computeDecodeTargetSize(
                    srcWidth = info.size.width,
                    srcHeight = info.size.height,
                    reqMaxSide = reqMaxSide
                )
                if (targetW > 0 && targetH > 0) {
                    decoder.setTargetSize(targetW, targetH)
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "ImageDecoder failed, falling back to BitmapFactory", error)
        }.getOrNull()
    }

    private fun decodeWithBitmapFactory(file: File, reqMaxSide: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                reqMaxSide
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        val orientation = readExifOrientation(file)
        val upright = applyExifOrientation(raw, orientation)
        if (upright !== raw && !raw.isRecycled) {
            raw.recycle()
        }
        return upright
    }

    private fun ensureSoftwareArgb8888(bitmap: Bitmap): Bitmap {
        val needsCopy = bitmap.config == Bitmap.Config.HARDWARE || bitmap.config != Bitmap.Config.ARGB_8888
        if (!needsCopy) return bitmap
        val converted = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        if (converted != null && converted !== bitmap) {
            if (!bitmap.isRecycled) bitmap.recycle()
            return converted
        }
        return bitmap
    }

    private fun readExifOrientation(file: File): Int = runCatching {
        ExifInterface(file.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
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

    /**
     * Renders [source] into a circular sprite of [sizePx] x [sizePx] using the exact same
     * pan/zoom placement geometry as the interactive crop preview.
     */
    private fun createCircularSprite(
        source: Bitmap,
        sizePx: Int,
        crop: BirdCropTransform
    ): Bitmap {
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val center = sizePx * 0.5f
        val borderWidth = sizePx * 0.055f
        val photoRadius = center - borderWidth
        val photoDiameter = photoRadius * 2f

        val placement = computeBirdCropPlacement(
            srcWidth = source.width,
            srcHeight = source.height,
            circleCenterX = center,
            circleCenterY = center,
            circleDiameter = photoDiameter,
            crop = crop
        )

        val shaderMatrix = Matrix().apply {
            setScale(placement.scale, placement.scale)
            postTranslate(placement.left, placement.top)
        }
        val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(shaderMatrix)
        }

        val shaderPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            this.shader = shader
        }
        canvas.drawCircle(center, center, photoRadius, shaderPaint)

        // Bright white inner ring for crisp readability against any background
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

        return output
    }

    private fun createBackgroundBitmap(source: Bitmap): Bitmap {
        val srcWidth = source.width.coerceAtLeast(1)
        val srcHeight = source.height.coerceAtLeast(1)
        val (targetWidth, targetHeight) = computeBackgroundTargetSize(srcWidth, srcHeight)
        val desiredAspect = targetWidth.toFloat() / targetHeight.toFloat()
        val cropBounds = computeCenterCropBounds(srcWidth, srcHeight, desiredAspect)

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        canvas.drawColor(BG_TRANSPARENCY_FALLBACK_COLOR)

        val srcRect = Rect(
            cropBounds.left,
            cropBounds.top,
            cropBounds.left + cropBounds.width,
            cropBounds.top + cropBounds.height
        )
        val dstRect = Rect(0, 0, targetWidth, targetHeight)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, srcRect, dstRect, paint)
        return output
    }

    private fun writeBitmapAtomically(
        bitmap: Bitmap,
        targetFile: File,
        format: Bitmap.CompressFormat,
        quality: Int
    ) {
        val tempFile = File(targetFile.parentFile, "${targetFile.name}.tmp")
        val compressed = FileOutputStream(tempFile).use { out ->
            val ok = bitmap.compress(format, quality, out)
            out.flush()
            ok
        }
        if (!compressed) {
            runCatching { tempFile.delete() }
            throw IOException("Gagal menyimpan cache gambar.")
        }
        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
    }

    internal data class CropBounds(
        val left: Int,
        val top: Int,
        val width: Int,
        val height: Int
    )

    data class BirdCropPlacement(
        val scale: Float,
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float
    )

    companion object {
        private const val TAG = "CustomMediaManager"
        private const val BIRD_AVATAR_PX = 256
        private const val BIRD_CROP_PREVIEW_MAX_PX = 1024
        const val MIN_BIRD_CROP_ZOOM = 1.0f
        const val MAX_BIRD_CROP_ZOOM = 4.0f
        internal const val BG_MAX_WIDTH_PX = 2160
        internal const val BG_MAX_HEIGHT_PX = 1080
        private const val BG_MIN_HEIGHT_PX = 480
        internal const val BG_MIN_ASPECT = 0.45f
        internal const val BG_MAX_ASPECT = 2.50f
        private const val MAX_IMPORT_FILE_BYTES = 35L * 1024L * 1024L
        private const val BG_TRANSPARENCY_FALLBACK_COLOR = 0xFF7BE0EA.toInt()
        private const val BIRD_CACHE_FILE = "custom_bird_avatar.png"
        private const val BG_CACHE_FILE = "custom_background.jpg"

        fun computeMaxBirdPanNormalized(
            srcWidth: Int,
            srcHeight: Int,
            zoom: Float
        ): Pair<Float, Float> {
            val safeW = srcWidth.coerceAtLeast(1).toFloat()
            val safeH = srcHeight.coerceAtLeast(1).toFloat()
            val minDim = min(safeW, safeH)
            val clampedZoom = zoom.coerceIn(MIN_BIRD_CROP_ZOOM, MAX_BIRD_CROP_ZOOM)
            val maxPanX = max(0f, ((safeW / minDim) * clampedZoom - 1f) * 0.5f)
            val maxPanY = max(0f, ((safeH / minDim) * clampedZoom - 1f) * 0.5f)
            return maxPanX to maxPanY
        }

        fun clampBirdCropTransform(
            srcWidth: Int,
            srcHeight: Int,
            crop: BirdCropTransform
        ): BirdCropTransform {
            val clampedZoom = crop.zoom.coerceIn(MIN_BIRD_CROP_ZOOM, MAX_BIRD_CROP_ZOOM)
            val (maxPanX, maxPanY) = computeMaxBirdPanNormalized(srcWidth, srcHeight, clampedZoom)
            return BirdCropTransform(
                zoom = clampedZoom,
                panNormX = crop.panNormX.coerceIn(-maxPanX, maxPanX),
                panNormY = crop.panNormY.coerceIn(-maxPanY, maxPanY)
            )
        }

        fun computeBirdCropPlacement(
            srcWidth: Int,
            srcHeight: Int,
            circleCenterX: Float,
            circleCenterY: Float,
            circleDiameter: Float,
            crop: BirdCropTransform
        ): BirdCropPlacement {
            val safeW = srcWidth.coerceAtLeast(1).toFloat()
            val safeH = srcHeight.coerceAtLeast(1).toFloat()
            val minDim = min(safeW, safeH)
            val clamped = clampBirdCropTransform(srcWidth, srcHeight, crop)

            val scale = (circleDiameter / minDim) * clamped.zoom
            val scaledW = safeW * scale
            val scaledH = safeH * scale
            val left = circleCenterX + clamped.panNormX * circleDiameter - scaledW * 0.5f
            val top = circleCenterY + clamped.panNormY * circleDiameter - scaledH * 0.5f
            return BirdCropPlacement(
                scale = scale,
                left = left,
                top = top,
                width = scaledW,
                height = scaledH
            )
        }

        internal fun calculateInSampleSize(width: Int, height: Int, reqMaxSide: Int): Int {
            var inSampleSize = 1
            val maxSide = max(width, height)
            while ((maxSide / inSampleSize) > reqMaxSide * 2) {
                inSampleSize *= 2
            }
            return inSampleSize
        }

        internal fun computeDecodeTargetSize(
            srcWidth: Int,
            srcHeight: Int,
            reqMaxSide: Int
        ): Pair<Int, Int> {
            if (srcWidth <= 0 || srcHeight <= 0 || reqMaxSide <= 0) return srcWidth to srcHeight
            val maxSide = max(srcWidth, srcHeight)
            if (maxSide <= reqMaxSide) return srcWidth to srcHeight
            val ratio = reqMaxSide.toFloat() / maxSide.toFloat()
            val targetW = (srcWidth * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (srcHeight * ratio).roundToInt().coerceAtLeast(1)
            return targetW to targetH
        }

        internal fun computeBackgroundTargetSize(srcWidth: Int, srcHeight: Int): Pair<Int, Int> {
            val safeW = srcWidth.coerceAtLeast(1)
            val safeH = srcHeight.coerceAtLeast(1)
            val rawAspect = safeW.toFloat() / safeH.toFloat()
            val desiredAspect = rawAspect.coerceIn(BG_MIN_ASPECT, BG_MAX_ASPECT)

            var targetHeight = min(safeH, BG_MAX_HEIGHT_PX).coerceAtLeast(BG_MIN_HEIGHT_PX)
            var targetWidth = (targetHeight * desiredAspect).roundToInt().coerceAtLeast(240)

            if (targetWidth > BG_MAX_WIDTH_PX) {
                targetWidth = BG_MAX_WIDTH_PX
                targetHeight = (targetWidth / desiredAspect).roundToInt().coerceAtLeast(360)
            }
            return targetWidth to targetHeight
        }

        internal fun computeCenterCropBounds(
            srcWidth: Int,
            srcHeight: Int,
            desiredAspect: Float
        ): CropBounds {
            val safeW = srcWidth.coerceAtLeast(1)
            val safeH = srcHeight.coerceAtLeast(1)
            val rawAspect = safeW.toFloat() / safeH.toFloat()
            val cropW: Int
            val cropH: Int
            if (rawAspect > desiredAspect) {
                cropH = safeH
                cropW = (safeH * desiredAspect).roundToInt().coerceIn(1, safeW)
            } else {
                cropW = safeW
                cropH = (safeW / desiredAspect).roundToInt().coerceIn(1, safeH)
            }
            val left = ((safeW - cropW) / 2).coerceAtLeast(0)
            val top = ((safeH - cropH) / 2).coerceAtLeast(0)
            return CropBounds(left = left, top = top, width = cropW, height = cropH)
        }
    }
}
