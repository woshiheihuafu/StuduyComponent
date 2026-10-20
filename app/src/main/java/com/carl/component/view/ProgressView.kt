package com.carl.component.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.carl.component.R
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.roundToInt


class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val STROKE_WIDTH = 5f // 圆环线条宽度
        private const val TOUCH_WIDTH = STROKE_WIDTH + 10f // 增加圆环线条的点击区域
    }

    /**
     * 模式
     * 0:圆弧
     * 1:条形
     */
    var mode: Int = 0

    /**
     * 进度
     */
    private var progress: Float = 0f

    /**
     * 默认
     */
    val den = resources.displayMetrics.density

    /**
     * 进度
     */
    private var progressAnimator: ValueAnimator? = null

    /**
     * 圆环进度条paint
     */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH * den
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * 圆环进度文字paint
     */

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        textSize = 0f
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    /**
     * 圆环进度条与文字的左右padding
     */
    private var textPadding = 0f

    /**
     * 是否处于拖动状态
     */
    private var isDragging = false

    private var rectF: RectF? = null

    private var activePointerId = -1 

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0)
        try {
            mode = ta.getInt(R.styleable.ProgressView_mode, 0)
            val color = ta.getColor(R.styleable.ProgressView_color, Color.BLUE)
            paint.color = color
            textPaint.color = color
            textPaint.textSize = ta.getDimension(R.styleable.ProgressView_textSize, 0f)
            textPadding = ta.getDimension(R.styleable.ProgressView_textPadding, 0f)
            progress = ta.getFloat(R.styleable.ProgressView_progress, 0f).coerceIn(0f, 100f)
        } finally {
            ta.recycle()
        }
    }

    private val defaultDiameter = (120f * den).roundToInt()

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        measureCircle(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDetachedFromWindow() {
        progressAnimator?.cancel()
        progressAnimator = null
        isDragging = false
        activePointerId = -1
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                Log.d("TouchTest", "DOWN")
                if (!isInCircle(event.x, event.y)) {
                    false
                } else {
                    activePointerId = event.getPointerId(0)
                    isDragging = true
                    progressAnimator?.cancel()
                    parent?.requestDisallowInterceptTouchEvent(true)
                    true
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 其他手指按下，不改变当前控制者
                true
            }

            MotionEvent.ACTION_MOVE -> {
                Log.d("TouchTest", "MOVE")
                val index = event.findPointerIndex(activePointerId)

                if (isDragging && index >= 0) {
                    calculateProgress(
                        event.getX(index),
                        event.getY(index)
                    )
                }
                true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val upIndex = event.actionIndex
                val upId = event.getPointerId(upIndex)

                if (upId == activePointerId) {
                    // 当前控制者抬起，切换到另一根手指
                    val newIndex = if (upIndex == 0) 1 else 0
                    activePointerId = event.getPointerId(newIndex)
                }
                true
            }

            MotionEvent.ACTION_UP -> {
                Log.d("TouchTest", "UP")
                val index = event.findPointerIndex(activePointerId)
                if (isDragging && index >= 0) {
                    calculateProgress(
                        event.getX(index),
                        event.getY(index)
                    )
                    performClick()
                }
                finishTouch()
                true
            }

            MotionEvent.ACTION_CANCEL -> {
                Log.d("TouchTest", "CANCEL")
                finishTouch()
                true
            }

            else -> super.onTouchEvent(event)
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun isInCircle(x: Float, y: Float): Boolean {
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        val centerX = paddingLeft + contentWidth / 2
        val centerY = paddingTop + contentHeight / 2
        val distance = hypot(x - centerX, y - centerY)
        val diameter = minOf(contentWidth, contentHeight)
        val radius = (diameter - paint.strokeWidth) / 2
        return abs(distance - radius) < TOUCH_WIDTH * den / 2
    }

    private fun calculateProgress(x: Float, y: Float) {
        if (!isInCircle(x, y) && !isDragging) return
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        val centerX = paddingLeft + contentWidth / 2
        val centerY = paddingTop + contentHeight / 2
        val dx = x - centerX
        val dy = y - centerY
        val standardAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val angle = (standardAngle + 90f + 360f) % 360f
        val progress = angle / 360f * 100f
        updateProgress(progress)
    }

    private fun measureCircle(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        if (textPaint.textSize != 0f) {
            measureCircleWithTextSize(
                widthMeasureSpec,
                heightMeasureSpec
            )
            return
        }
        val desiredWidth = maxOf(
            defaultDiameter + paddingLeft + paddingRight,
            suggestedMinimumWidth
        )
        val desiredHeight = maxOf(
            defaultDiameter + paddingTop + paddingBottom,
            suggestedMinimumHeight
        )
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    private fun measureCircleWithTextSize(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val textWidth = textPaint.measureText("100%")

        val diameter = (
                textWidth +
                        textPadding * 2 +
                        paint.strokeWidth
                ).roundToInt()

        val desiredWidth = maxOf(
            diameter + paddingLeft + paddingRight,
            suggestedMinimumWidth
        )

        val desiredHeight = maxOf(
            diameter + paddingTop + paddingBottom,
            suggestedMinimumHeight
        )

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawCircle(canvas)
    }

    private fun drawCircle(canvas: Canvas) {

        if (textPaint.textSize != 0f) {
            drawCircleWithTextSize(canvas)
            return
        }
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val diameter = minOf(contentWidth, contentHeight) -
                paint.strokeWidth

        if (diameter <= 0f) return
        val radius = diameter / 2f
        val centerX = paddingLeft + contentWidth / 2f
        val centerY = paddingTop + contentHeight / 2f
        if (rectF == null) {
            rectF = RectF(
                centerX - radius,
                centerY - radius,
                centerX + radius,
                centerY + radius
            )
        }
        val startAngle = -90f
        val sweepAngle = progress / 100f * 360f
        setTextSize(diameter - textPadding)

        canvas.drawText(
            "${progress.toInt()}%",
            centerX,
            getBaseLine(centerY, textPaint.fontMetrics),
            textPaint
        )
        canvas.drawArc(rectF!!, startAngle, sweepAngle, false, paint)
    }

    private fun drawCircleWithTextSize(canvas: Canvas) {
        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom
        val diameter = minOf(contentWidth, contentHeight) - paint.strokeWidth
        if (diameter <= 0) return
        val radius = diameter / 2
        val centerX = paddingLeft + contentWidth / 2f
        val centerY = paddingTop + contentHeight / 2f
        val rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
        val startAngle = -90f
        val sweepAngle = progress / 100f * 360f
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
        canvas.drawText(
            "${progress.toInt()}%",
            centerX,
            getBaseLine(centerY, textPaint.fontMetrics),
            textPaint
        )
    }

    private fun setTextSize(contentWidth: Float) {
        var textWidth = textPaint.measureText("100%")
        while (textWidth < contentWidth) {
            textPaint.textSize = (textPaint.textSize / den + 1f) * den
            textWidth = textPaint.measureText("100%")
        }
        textPaint.textSize = (textPaint.textSize / den - 1f) * den
    }

    private fun getBaseLine(centerY: Float, fontMetrics: Paint.FontMetrics): Float {
        val top = centerY + fontMetrics.ascent
        val bottom = centerY + fontMetrics.descent
        val mid = (top + bottom) / 2
        return centerY - (mid - centerY)
    }

    private fun finishTouch() {
        isDragging = false
        activePointerId = -1
        parent?.requestDisallowInterceptTouchEvent(false)
    }

    fun updateProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    fun animateProgressTo(
        target: Float,
        durationMs: Long = 1000L
    ) {
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, target.coerceIn(0f, 100f)).apply {
            duration = durationMs
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                updateProgress(animatedValue)
            }
            start()
        }
    }

    fun animateProgress() {
        animateProgressTo(100f, 3000L)
    }
}