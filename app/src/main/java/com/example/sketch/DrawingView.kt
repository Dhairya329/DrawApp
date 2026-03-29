package com.example.sketch

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import android.graphics.Path
import android.util.TypedValue

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    // Drawing path
    private lateinit var drawPath: FingerPath

    // Define what to draw
    private lateinit var canvasPaint: Paint

    // Define how to draw
    private lateinit var drawPaint: Paint
    private var color = Color.BLACK
    private var brushSize = 0.toFloat()
    private lateinit var canvas: Canvas
    private lateinit var canvasBitmap: Bitmap
    private val paths = mutableListOf<FingerPath>()

    init {
        setupDrawing()
    }

    private fun setupDrawing() {

        drawPaint = Paint()
        drawPath = FingerPath(color, brushSize)
        drawPaint.color = color
        drawPaint.style = Paint.Style.STROKE
        drawPaint.strokeJoin = Paint.Join.ROUND
        drawPaint.strokeCap = Paint.Cap.ROUND
        brushSize = 15.toFloat()
        canvasPaint = Paint(Paint.DITHER_FLAG)
    }

    fun changeBrushSize(newSize: Float) {

        brushSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            newSize,
            resources.displayMetrics
        )
        drawPaint.strokeWidth = brushSize
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {

        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(canvasBitmap)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x   // Touch event x coordinate
        val touchY = event?.y   // Touch event y coordinate

        when (event?.action) {

            // This event is triggered when the user first touches the screen
            MotionEvent.ACTION_DOWN -> {
                drawPath.color = color
                drawPath.brushThickness = brushSize

                drawPath.reset()  // Clear the path
                drawPath.moveTo(touchX!!, touchY!!)  // Move to the touched point
            }

            // This event is triggered when the user moves their finger on the screen
            MotionEvent.ACTION_MOVE -> {
                drawPath.lineTo(touchX!!, touchY!!)  // Draw a line to the new touched point
            }

            MotionEvent.ACTION_UP -> {
                paths.add(drawPath) // Add the path to the list of paths
                drawPath = FingerPath(color, brushSize) // Draw the path on the canvas
            }

            else -> return false
        }

        invalidate()
        return true
    }

    override fun onDraw(canvas: Canvas) {

        super.onDraw(canvas)
        canvas.drawBitmap(canvasBitmap, 0f, 0f, drawPaint)

        for (path in paths) {
            drawPaint.strokeWidth = path.brushThickness
            drawPaint.color = path.color
            canvas.drawPath(path, drawPaint) // Drawing path on canvas
        }

        if (!drawPath.isEmpty) {
            drawPaint.strokeWidth = drawPath.brushThickness
            drawPaint.color = drawPath.color
            canvas.drawPath(drawPath, drawPaint) // Drawing path on canvas
        }
    }

    fun setColor(newColor: Any) {

        if (newColor is String) {
            color = Color.parseColor(newColor)
            drawPaint.color = color
        } else if (newColor is Int) {
            color = newColor
            drawPaint.color = color
        }
    }

    fun undo() {

        if (paths.isNotEmpty()) {
            paths.removeAt(paths.size - 1) // Remove the last path from the list
            invalidate() // Redraw the view
        }
    }

    internal inner class FingerPath(var color: Int, var brushThickness: Float) : Path()
}











