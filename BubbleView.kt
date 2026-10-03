package com.example.spacelens

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

data class BubbleNode(
    val file: FileNode,
    var x: Float = 0f,
    var y: Float = 0f,
    var radius: Float = 0f,
    val color: Int
)

class BubbleView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val bubbles = mutableListOf<BubbleNode>()
    private var onBubbleClick: ((FileNode) -> Unit)? = null

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#40FFFFFF")
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CCFFFFFF")
        textAlign = Paint.Align.CENTER
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0F0FF")
    }

    private val palette = intArrayOf(
        0xFF5B3E96.toInt(), 0xFF7B5FB8.toInt(),
        0xFF6B4BA8.toInt(), 0xFF8B6FC8.toInt(),
        0xFF4B2E86.toInt(), 0xFF9D7CE8.toInt()
    )

    fun setNodes(nodes: List<FileNode>) {
        bubbles.clear()
        val maxSize = nodes.maxOfOrNull { it.size } ?: 1L

        nodes.take(12).forEachIndexed { i, node ->
            val ratio = (node.size.toFloat() / maxSize.toFloat()).coerceIn(0.15f, 1f)
            bubbles.add(
                BubbleNode(
                    file = node,
                    radius = 0f, // will compute on layout
                    color = palette[i % palette.size]
                ).apply { this.radius = ratio }
            )
        }
        requestLayout()
        invalidate()
    }

    fun setOnBubbleClick(l: (FileNode) -> Unit) { onBubbleClick = l }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutBubbles(w.toFloat(), h.toFloat())
    }

    private fun layoutBubbles(w: Float, h: Float) {
        if (bubbles.isEmpty()) return
        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = (minOf(w, h) * 0.30f)

        // Sort: largest at center, then orbit around
        val sorted = bubbles.sortedByDescending { it.radius }
        val centerNode = sorted.firstOrNull()

        centerNode?.let {
            it.x = cx
            it.y = cy
            it.radius = maxRadius
        }

        val rest = sorted.drop(1)
        if (rest.isEmpty()) return

        // Arrange remaining in rings
        val ring1 = rest.take(4)
        val ring2 = rest.drop(4)

        ring1.forEachIndexed { i, node ->
            val angle = (Math.PI * 2 * i / ring1.size) - Math.PI / 2
            val dist = maxRadius * 1.15f
            node.x = cx + (cos(angle) * dist).toFloat()
            node.y = cy + (sin(angle) * dist).toFloat()
            node.radius = maxRadius * 0.62f * node.radius.coerceIn(0.5f, 1f)
        }

        ring2.forEachIndexed { i, node ->
            val angle = (Math.PI * 2 * i / ring2.size.coerceAtLeast(1)) - Math.PI / 4
            val dist = maxRadius * 1.75f
            node.x = cx + (cos(angle) * dist).toFloat()
            node.y = cy + (sin(angle) * dist).toFloat()
            node.radius = maxRadius * 0.42f * node.radius.coerceIn(0.4f, 1f)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw soft glow background
        for (b in bubbles) {
            // Outer glow
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.FILL
                shader = RadialGradient(
                    b.x, b.y, b.radius * 1.3f,
                    intArrayOf((b.color and 0x00FFFFFF) or 0x40000000, Color.TRANSPARENT),
                    floatArrayOf(0.6f, 1f), Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(b.x, b.y, b.radius * 1.3f, glowPaint)
        }

        // Draw bubbles
        for (b in bubbles) {
            // Main fill with gradient
            fillPaint.shader = RadialGradient(
                b.x - b.radius * 0.3f, b.y - b.radius * 0.3f, b.radius * 1.2f,
                intArrayOf(lighten(b.color, 0.35f), b.color, darken(b.color, 0.3f)),
                floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(b.x, b.y, b.radius, fillPaint)
            canvas.drawCircle(b.x, b.y, b.radius, strokePaint)

            // Folder icon
            drawFolderIcon(canvas, b)

            // Label
            textPaint.textSize = (b.radius * 0.20f).coerceIn(14f, 32f)
            subTextPaint.textSize = (b.radius * 0.14f).coerceIn(11f, 20f)

            canvas.drawText(
                b.file.name.take(12),
                b.x, b.y + b.radius * 0.55f, textPaint
            )
            canvas.drawText(
                formatSize(b.file.size),
                b.x, b.y + b.radius * 0.78f, subTextPaint
            )
        }
    }

    private fun drawFolderIcon(canvas: Canvas, b: BubbleNode) {
        val iconSize = b.radius * 0.55f
        val left = b.x - iconSize / 2f
        val top = b.y - iconSize * 0.65f
        val right = b.x + iconSize / 2f
        val bottom = b.y + iconSize * 0.1f

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8DCF0")
            style = Paint.Style.FILL
        }
        val rect = RectF(left, top, right, bottom)
        val r = iconSize * 0.12f
        canvas.drawRoundRect(rect, r, r, iconPaint)

        // Folder tab
        val tabPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8FC4EC")
            style = Paint.Style.FILL
        }
        val tab = RectF(left, top - iconSize * 0.12f, left + iconSize * 0.45f, top + iconSize * 0.05f)
        canvas.drawRoundRect(tab, r, r, tabPaint)
    }

    private fun lighten(color: Int, f: Float): Int = Color.argb(
        Color.alpha(color),
        (Color.red(color) + (255 - Color.red(color)) * f).toInt().coerceAtMost(255),
        (Color.green(color) + (255 - Color.green(color)) * f).toInt().coerceAtMost(255),
        (Color.blue(color) + (255 - Color.blue(color)) * f).toInt().coerceAtMost(255)
    )

    private fun darken(color: Int, f: Float): Int = Color.argb(
        Color.alpha(color),
        (Color.red(color) * (1 - f)).toInt(),
        (Color.green(color) * (1 - f)).toInt(),
        (Color.blue(color) * (1 - f)).toInt()
    )

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format("%.1f KB", kb)
        val mb = kb / 1024.0
        if (mb < 1024) return String.format("%.1f MB", mb)
        val gb = mb / 1024.0
        return String.format("%.1f GB", gb)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            for (b in bubbles) {
                val dx = event.x - b.x
                val dy = event.y - b.y
                if (dx * dx + dy * dy <= b.radius * b.radius) {
                    onBubbleClick?.invoke(b.file)
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean = super.performClick()
}