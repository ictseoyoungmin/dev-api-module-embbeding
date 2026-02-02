package com.poc.petgalleryxml.ui.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.poc.petgalleryxml.data.api.dto.InstanceOut
import kotlin.math.min

class BoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onInstanceClick: ((InstanceOut) -> Unit)? = null

    private var instances: List<InstanceOut> = emptyList()
    private var selectedId: String? = null

    private var imgW: Float = 0f
    private var imgH: Float = 0f

    private val paintBox = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.YELLOW
    }

    private val paintBoxDim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(180, 255, 255, 255)
    }

    private val paintTextBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(170, 0, 0, 0)
    }

    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textSize = 34f
    }

    private var displayRect: RectF? = null

    fun setImageIntrinsicSize(widthPx: Int, heightPx: Int) {
        imgW = widthPx.toFloat()
        imgH = heightPx.toFloat()
        invalidate()
    }

    fun setInstances(list: List<InstanceOut>) {
        instances = list
        invalidate()
    }

    fun setSelected(instanceId: String?) {
        selectedId = instanceId
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        val rect = computeDisplayRect(viewW, viewH)
        displayRect = rect

        for ((idx, ins) in instances.withIndex()) {
            val r = bboxToRect(ins, rect)
            val isSel = (ins.instanceId == selectedId)
            canvas.drawRect(r, if (isSel) paintBox else paintBoxDim)

            // Label text
            val label = buildString {
                append("#")
                append(idx + 1)
                append(" ")
                append(if (ins.species == "DOG") "🐶" else "🐱")
                if (!ins.petId.isNullOrBlank()) {
                    append(" ")
                    append(ins.petId)
                }
            }

            val textW = paintText.measureText(label)
            val textH = paintText.textSize
            val bg = RectF(r.left, r.top - textH - 12f, r.left + textW + 18f, r.top)
            canvas.drawRoundRect(bg, 10f, 10f, paintTextBg)
            canvas.drawText(label, r.left + 9f, r.top - 10f, paintText)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        val rect = displayRect ?: return true

        val x = event.x
        val y = event.y
        if (!rect.contains(x, y)) return true

        // find top-most/first match; optionally choose smallest area match
        val hit = instances
            .map { it to bboxToRect(it, rect) }
            .filter { (_, r) -> r.contains(x, y) }
            .minByOrNull { (_, r) -> r.width() * r.height() }
            ?.first

        if (hit != null) {
            selectedId = hit.instanceId
            invalidate()
            onInstanceClick?.invoke(hit)
        }
        return true
    }

    private fun computeDisplayRect(viewW: Float, viewH: Float): RectF {
        if (imgW <= 0f || imgH <= 0f) {
            // fallback: assume full view
            return RectF(0f, 0f, viewW, viewH)
        }
        val scale = min(viewW / imgW, viewH / imgH)
        val dispW = imgW * scale
        val dispH = imgH * scale
        val left = (viewW - dispW) / 2f
        val top = (viewH - dispH) / 2f
        return RectF(left, top, left + dispW, top + dispH)
    }

    private fun bboxToRect(ins: InstanceOut, disp: RectF): RectF {
        val bw = disp.width()
        val bh = disp.height()
        val x1 = disp.left + (ins.bbox.x1.toFloat() * bw)
        val y1 = disp.top + (ins.bbox.y1.toFloat() * bh)
        val x2 = disp.left + (ins.bbox.x2.toFloat() * bw)
        val y2 = disp.top + (ins.bbox.y2.toFloat() * bh)
        return RectF(x1, y1, x2, y2)
    }
}
