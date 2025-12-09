package com.example.clothsearch

import android.graphics.Color
import kotlin.math.pow
import kotlin.math.sqrt

private data class Named(val name: String, val rgb: Int)

private val BASIC = listOf(
    Named("black", Color.rgb(0, 0, 0)),
    Named("white", Color.rgb(255, 255, 255)),
    Named("gray", Color.rgb(128, 128, 128)),
    Named("red", Color.rgb(220, 20, 60)),
    Named("orange", Color.rgb(255, 140, 0)),
    Named("yellow", Color.rgb(255, 215, 0)),
    Named("green", Color.rgb(34, 139, 34)),
    Named("blue", Color.rgb(30, 144, 255)),
    Named("navy", Color.rgb(0, 0, 128)),
    Named("purple", Color.rgb(128, 0, 128)),
    Named("brown", Color.rgb(139, 69, 19)),
    Named("beige", Color.rgb(245, 245, 220))
)

fun nearestBasicColorName(rgb: Int): String {
    val r = Color.red(rgb).toDouble()
    val g = Color.green(rgb).toDouble()
    val b = Color.blue(rgb).toDouble()
    var best = BASIC.first()
    var bestD = Double.MAX_VALUE
    for (c in BASIC) {
        val dr = r - Color.red(c.rgb)
        val dg = g - Color.green(c.rgb)
        val db = b - Color.blue(c.rgb)
        val d = sqrt(dr.pow(2) + dg.pow(2) + db.pow(2))
        if (d < bestD) {
            bestD = d
            best = c
        }
    }
    return best.name
}
