package com.aura.feature.home.presentation.components.mesh

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import com.aura.feature.home.domain.model.GeoPoint
import kotlin.math.roundToInt

internal object WorldLandmass {

    fun dotGrid(projection: WorldProjection, columns: Int): List<Offset> {
        val rows = (columns / projection.aspectRatio).roundToInt().coerceAtLeast(1)
        val mask = rasterizeLandmass(projection, columns, rows)

        val dots = ArrayList<Offset>(columns * rows / 4)
        for (row in 0 until rows) {
            for (column in 0 until columns) {
                val alpha = mask[row * columns + column] ushr 24
                if (alpha > ALPHA_THRESHOLD) {
                    dots += Offset(
                        x = (column + 0.5f) / columns,
                        y = (row + 0.5f) / rows,
                    )
                }
            }
        }
        return dots
    }

    private fun rasterizeLandmass(
        projection: WorldProjection,
        columns: Int,
        rows: Int,
    ): IntArray {
        val bitmap = Bitmap.createBitmap(columns, rows, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeWidth = 0.7f
            color = android.graphics.Color.WHITE
        }

        OUTLINES.forEach { outline ->
            canvas.drawPath(outline.toPath(projection, columns, rows), paint)
        }

        val pixels = IntArray(columns * rows)
        bitmap.getPixels(pixels, 0, columns, 0, 0, columns, rows)
        bitmap.recycle()
        return pixels
    }

    private fun DoubleArray.toPath(
        projection: WorldProjection,
        width: Int,
        height: Int,
    ): Path {
        val path = Path()
        var index = 0
        while (index < size) {
            val normalized = projection.normalize(
                GeoPoint(latitude = this[index + 1], longitude = this[index])
            )
            val x = normalized.x * width
            val y = normalized.y * height
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            index += 2
        }
        path.close()
        return path
    }

    private const val ALPHA_THRESHOLD = 60

    private val OUTLINES: List<DoubleArray> = listOf(
        doubleArrayOf(
            -168.0, 66.0, -165.0, 60.0, -158.0, 56.0, -152.0, 59.0, -146.0, 60.0,
            -136.0, 58.0, -130.0, 54.0, -124.0, 48.0, -124.0, 41.0, -121.0, 35.0,
            -117.0, 32.0, -114.0, 28.0, -110.0, 24.0, -105.0, 20.0, -99.0, 17.0,
            -96.0, 16.0, -94.0, 18.0, -91.0, 18.0, -88.0, 21.0, -90.0, 25.0,
            -97.0, 26.0, -94.0, 29.0, -89.0, 29.0, -84.0, 30.0, -80.0, 26.0,
            -81.0, 31.0, -76.0, 35.0, -74.0, 39.0, -70.0, 42.0, -66.0, 44.0,
            -60.0, 46.0, -53.0, 47.0, -56.0, 52.0, -64.0, 60.0, -68.0, 62.0,
            -75.0, 68.0, -80.0, 73.0, -90.0, 73.0, -100.0, 70.0, -110.0, 68.0,
            -125.0, 70.0, -135.0, 69.0, -150.0, 70.0, -162.0, 70.0,
        ),
        doubleArrayOf(
            -92.0, 15.0, -88.0, 16.0, -84.0, 11.0, -79.0, 9.0,
            -77.0, 8.0, -83.0, 9.0, -87.0, 13.0, -91.0, 14.0,
        ),
        doubleArrayOf(
            -77.0, 8.0, -72.0, 12.0, -62.0, 11.0, -55.0, 6.0, -50.0, 1.0,
            -44.0, -2.0, -38.0, -6.0, -35.0, -8.0, -39.0, -14.0, -41.0, -22.0,
            -48.0, -26.0, -53.0, -34.0, -58.0, -38.0, -62.0, -41.0, -65.0, -45.0,
            -68.0, -50.0, -70.0, -55.0, -74.0, -52.0, -75.0, -46.0, -74.0, -40.0,
            -72.0, -33.0, -71.0, -25.0, -70.0, -18.0, -76.0, -14.0, -81.0, -6.0,
            -80.0, -2.0, -78.0, 1.0, -77.0, 7.0,
        ),
        doubleArrayOf(
            5.0, 62.0, 12.0, 66.0, 20.0, 70.0, 30.0, 70.0, 40.0, 68.0,
            55.0, 70.0, 70.0, 73.0, 80.0, 74.0, 95.0, 76.0, 105.0, 77.0,
            115.0, 74.0, 130.0, 73.0, 140.0, 72.0, 155.0, 70.0, 170.0, 69.0,
            180.0, 66.0, 178.0, 62.0, 165.0, 60.0, 160.0, 58.0, 155.0, 52.0,
            145.0, 50.0, 140.0, 45.0, 132.0, 43.0, 127.0, 38.0, 122.0, 32.0,
            118.0, 25.0, 110.0, 21.0, 105.0, 10.0, 108.0, 4.0, 104.0, 1.0,
            100.0, 8.0, 97.0, 16.0, 94.0, 22.0, 90.0, 22.0, 88.0, 20.0,
            80.0, 10.0, 77.0, 8.0, 72.0, 20.0, 68.0, 24.0, 66.0, 25.0,
            62.0, 25.0, 58.0, 23.0, 57.0, 20.0, 52.0, 16.0, 45.0, 12.0,
            43.0, 13.0, 39.0, 20.0, 34.0, 28.0, 34.0, 31.0, 36.0, 36.0,
            30.0, 36.0, 26.0, 36.0, 23.0, 38.0, 20.0, 40.0, 18.0, 43.0,
            13.0, 45.0, 18.0, 40.0, 16.0, 38.0, 12.0, 42.0, 10.0, 44.0,
            7.0, 43.0, 3.0, 42.0, 0.0, 39.0, -5.0, 36.0, -9.0, 38.0,
            -9.0, 43.0, -2.0, 43.0, -1.0, 46.0, -4.0, 48.0, 0.0, 50.0,
            3.0, 53.0, 8.0, 54.0, 10.0, 57.0, 12.0, 56.0, 10.0, 59.0,
        ),
        doubleArrayOf(
            -6.0, 36.0, 0.0, 37.0, 10.0, 37.0, 20.0, 33.0, 25.0, 32.0,
            32.0, 31.0, 35.0, 28.0, 38.0, 18.0, 43.0, 12.0, 51.0, 12.0,
            51.0, 10.0, 45.0, 5.0, 42.0, 0.0, 40.0, -5.0, 40.0, -15.0,
            35.0, -20.0, 32.0, -26.0, 28.0, -33.0, 20.0, -35.0, 18.0, -34.0,
            14.0, -23.0, 12.0, -16.0, 9.0, -1.0, 9.0, 4.0, 3.0, 6.0,
            -5.0, 5.0, -10.0, 6.0, -14.0, 11.0, -17.0, 15.0, -17.0, 21.0,
            -13.0, 28.0, -10.0, 31.0,
        ),
        doubleArrayOf(
            113.0, -22.0, 114.0, -26.0, 116.0, -32.0, 119.0, -34.0, 126.0, -32.0,
            132.0, -32.0, 137.0, -35.0, 140.0, -38.0, 146.0, -39.0, 150.0, -37.0,
            153.0, -30.0, 153.0, -25.0, 146.0, -19.0, 142.0, -11.0, 137.0, -12.0,
            131.0, -12.0, 126.0, -14.0, 122.0, -17.0, 117.0, -20.0,
        ),
        doubleArrayOf(
            -45.0, 60.0, -52.0, 64.0, -55.0, 68.0, -58.0, 72.0, -55.0, 76.0,
            -45.0, 80.0, -30.0, 83.0, -22.0, 80.0, -20.0, 75.0, -25.0, 70.0,
            -35.0, 66.0, -42.0, 61.0,
        ),
        doubleArrayOf(
            -180.0, -70.0, -150.0, -74.0, -120.0, -73.0, -100.0, -72.0, -80.0, -72.0,
            -65.0, -66.0, -58.0, -63.0, -62.0, -70.0, -45.0, -73.0, -20.0, -71.0,
            0.0, -70.0, 20.0, -70.0, 40.0, -68.0, 60.0, -67.0, 80.0, -66.0,
            100.0, -66.0, 120.0, -66.0, 140.0, -67.0, 160.0, -72.0, 180.0, -70.0,
            180.0, -74.0, -180.0, -74.0,
        ),
        doubleArrayOf(-5.0, 50.0, 0.0, 51.0, 2.0, 53.0, -1.0, 56.0, -5.0, 58.0, -6.0, 55.0),
        doubleArrayOf(-10.0, 52.0, -6.0, 52.0, -6.0, 55.0, -10.0, 55.0),
        doubleArrayOf(-24.0, 65.0, -14.0, 66.0, -14.0, 64.0, -22.0, 63.0),
        doubleArrayOf(44.0, -12.0, 50.0, -15.0, 50.0, -20.0, 47.0, -25.0, 44.0, -22.0, 43.0, -16.0),
        doubleArrayOf(
            130.0, 31.0, 135.0, 34.0, 140.0, 36.0, 142.0, 40.0,
            145.0, 44.0, 141.0, 45.0, 138.0, 37.0, 133.0, 34.0,
        ),
        doubleArrayOf(
            172.0, -34.0, 176.0, -38.0, 178.0, -38.0, 174.0, -41.0, 170.0, -44.0,
            167.0, -46.0, 166.0, -45.0, 170.0, -42.0, 172.0, -38.0,
        ),
        doubleArrayOf(95.0, 5.0, 100.0, 2.0, 104.0, -2.0, 106.0, -6.0, 103.0, -5.0, 98.0, 1.0),
        doubleArrayOf(105.0, -6.0, 114.0, -8.0, 114.0, -9.0, 105.0, -7.0),
        doubleArrayOf(109.0, 2.0, 117.0, 4.0, 119.0, 0.0, 116.0, -4.0, 110.0, -3.0),
        doubleArrayOf(119.0, 1.0, 125.0, 1.0, 125.0, -5.0, 121.0, -5.0, 120.0, -2.0),
        doubleArrayOf(
            131.0, -1.0, 140.0, -3.0, 147.0, -6.0, 150.0, -9.0,
            143.0, -9.0, 138.0, -8.0, 133.0, -4.0,
        ),
        doubleArrayOf(120.0, 18.0, 122.0, 14.0, 126.0, 10.0, 126.0, 6.0, 122.0, 7.0, 120.0, 13.0),
        doubleArrayOf(80.0, 9.0, 82.0, 8.0, 81.0, 6.0, 80.0, 7.0),
        doubleArrayOf(-85.0, 22.0, -77.0, 20.0, -74.0, 20.0, -84.0, 23.0),
        doubleArrayOf(145.0, -41.0, 148.0, -41.0, 148.0, -43.0, 145.0, -43.0),
    )
}
