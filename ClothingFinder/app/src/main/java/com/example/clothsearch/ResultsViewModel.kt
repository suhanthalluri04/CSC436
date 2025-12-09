package com.example.clothsearch

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResultsViewModel : ViewModel() {
    private val _cameraPermissionGranted = MutableStateFlow(false)
    val cameraPermissionGranted: StateFlow<Boolean> = _cameraPermissionGranted

    private val _photoBitmap = MutableStateFlow<Bitmap?>(null)
    val photoBitmap: StateFlow<Bitmap?> = _photoBitmap

    private val _lastImageUri = MutableStateFlow<String?>(null)
    val lastImageUri: StateFlow<String?> = _lastImageUri

    private val _selectedCategory = MutableStateFlow("")
    val selectedCategory: StateFlow<String> = _selectedCategory

    private val _dominantColorName = MutableStateFlow("")
    val dominantColorName: StateFlow<String> = _dominantColorName

    private val _queryText = MutableStateFlow("")
    val queryText: StateFlow<String> = _queryText

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    fun onCameraPermission(granted: Boolean) {
        _cameraPermissionGranted.value = granted
    }

    fun onPhotoCaptured(uri: Uri) {
        viewModelScope.launch {
            _isProcessing.value = true
            _lastImageUri.value = uri.toString()
            _photoBitmap.value = decode(uri)
            val bmp = _photoBitmap.value
            if (bmp != null) {
                val label = runMlKit(bmp)
                _selectedCategory.value = label
                _dominantColorName.value = extractColorName(bmp)
                _queryText.value = buildQuery(_dominantColorName.value, _selectedCategory.value)
            }
            _isProcessing.value = false
        }
    }

    private fun decode(uri: Uri): Bitmap? =
        runCatching {
            appResolver?.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
        }.getOrNull()

    companion object {
        var appResolver: ContentResolver? = null
    }

    private suspend fun runMlKit(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        val labels =
            kotlinx.coroutines.suspendCancellableCoroutine<List<com.google.mlkit.vision.label.ImageLabel>> { cont ->
                labeler.process(image)
                    .addOnSuccessListener { cont.resume(it) {} }
                    .addOnFailureListener { cont.resume(emptyList()) {} }
            }
        val allowed = setOf(
            "shirt",
            "t-shirt",
            "t shirt",
            "tee",
            "top",
            "hoodie",
            "sweatshirt",
            "sweater",
            "jumper",
            "pullover",
            "cap",
            "hat",
            "beanie",
            "pants",
            "trousers",
            "jeans",
            "shorts",
            "skirt",
            "dress",
            "jacket",
            "coat",
            "blazer",
            "shoe",
            "sneaker",
            "boots",
            "boot",
            "sandals"
        )
        val aliases = mapOf(
            "t shirt" to "t-shirt",
            "tee" to "t-shirt",
            "top" to "shirt",
            "jumper" to "sweater",
            "pullover" to "sweater",
            "boot" to "boots",
            "beanie" to "hat"
        )

        val best = labels
            .sortedByDescending { it.confidence }
            .map { it.text.lowercase() }
            .firstOrNull { lbl ->
                lbl in allowed || allowed.any { lbl.contains(it) }
            }
            ?.let { aliases[it] ?: it }

        val generic = setOf("clothing", "apparel", "garment", "clothes", "wear")
        val first = labels.firstOrNull()?.text?.lowercase()

        if (best != null) return best
        if (first != null && first !in generic) return first
        return "shirt"
    }

    private fun extractColorName(bitmap: Bitmap): String {
        val rgb = centerAverageColor(bitmap)
        val hsv = FloatArray(3)
        Color.colorToHSV(rgb, hsv)

        val sat = hsv[1]
        val value = hsv[2]
        if (value < 0.25f) return "black"
        if (sat < 0.08f && value < 0.4f) return "gray"
        if (sat < 0.08f && value > 0.85f) return "white"

        val swatch = Palette.from(bitmap).clearTargets().generate().dominantSwatch
        val paletteRgb = swatch?.rgb
        return nearestBasicColorName(paletteRgb ?: rgb)
    }

    private fun buildQuery(color: String, category: String): String =
        listOf(color, category)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    private fun centerAverageColor(bitmap: Bitmap): Int {
        val width = bitmap.width
        val height = bitmap.height
        val startX = (width * 0.25f).toInt()
        val startY = (height * 0.25f).toInt()
        val endX = (width * 0.75f).toInt()
        val endY = (height * 0.75f).toInt()
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0L
        for (y in startY until endY step 4) {
            for (x in startX until endX step 4) {
                val c = bitmap.getPixel(x, y)
                rSum += Color.red(c)
                gSum += Color.green(c)
                bSum += Color.blue(c)
                count++
            }
        }
        if (count == 0L) return 0xFF444444.toInt()
        return Color.rgb(
            (rSum / count).toInt(),
            (gSum / count).toInt(),
            (bSum / count).toInt()
        )
    }

    fun currentSavedSearch(): SavedSearch? {
        val query = _queryText.value
        val category = _selectedCategory.value
        val color = _dominantColorName.value
        if (query.isBlank() || category.isBlank() || color.isBlank()) return null
        return SavedSearch(
            imageUri = _lastImageUri.value,
            category = category,
            colorName = color,
            queryText = query,
            timestampMs = System.currentTimeMillis()
        )
    }

    fun loadSavedSearch(saved: SavedSearch) {
        viewModelScope.launch {
            _queryText.value = saved.queryText
            _selectedCategory.value = saved.category
            _dominantColorName.value = saved.colorName
            _lastImageUri.value = saved.imageUri
            _photoBitmap.value = saved.imageUri?.let { uri ->
                runCatching { decode(Uri.parse(uri)) }.getOrNull()
            }
            _isProcessing.value = false
        }
    }
}
