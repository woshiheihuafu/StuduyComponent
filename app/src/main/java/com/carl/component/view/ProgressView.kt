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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt


class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val STROKE_WIDTH = 5f // 圆环线条宽度
        private const val TICK_STROKE_WIDTH = 2f // 刻度线宽度
        private const val TOUCH_WIDTH = STROKE_WIDTH + 10f // 增加圆环线条的点击区域
        private const val TICK_LENGTH = 7f // 刻度线长度
        private const val TICK_PADDING = 7f //刻度线与进度条距离
        private const val PER_TICK_ANGLE = 30
        private const val TOTAL_TICK_COUNT = 12
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
     * 进度动画
     */
    private var progressAnimator: ValueAnimator? = null

    /**
     * 圆环进度条paint
     */
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = STROKE_WIDTH * den
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * 圆环进度文字paint
     */

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        textSize = 0f * den
        textAlign = Paint.Align.CENTER
        style = Paint.Style.FILL
    }

    /**
     * 刻度线paint：基础色
     */

    private val baseTickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = TICK_STROKE_WIDTH * den
        strokeCap = Paint.Cap.ROUND
    }

    /**
     * 刻度线paint：彩色
     */

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = TICK_STROKE_WIDTH * den
        strokeCap = Paint.Cap.ROUND
    }

    private var isDependOnTextSize = false

    /**
     * 是否处于拖动状态
     */
    private var isDragging = false

    /**
     * 尺寸成员变量
     */
    private var textPadding = 0f // 文字与刻度线的距离
    private var tickPadding = TICK_PADDING * den // 刻度线与进度条的距离

    private var tickLength = TICK_LENGTH * den
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    private var rectF: RectF = RectF()
    private var lastPaddingLeft = paddingLeft
    private var lastPaddingTop = paddingTop
    private var lastPaddingRight = paddingRight
    private var lastPaddingBottom = paddingBottom

    private var baseLine = 0f

    private var activePointerId = -1

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0)
        try {
            mode = ta.getInt(R.styleable.ProgressView_mode, 0)
            val color = ta.getColor(R.styleable.ProgressView_color, Color.BLUE)
            paint.color = color
            textPaint.color = color
            textPaint.textSize = ta.getDimension(R.styleable.ProgressView_textSize, textPaint.textSize)
            tickPaint.color = color
            if (textPaint.textSize != 0f) isDependOnTextSize = true
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
        finishTouch()
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDrawingCache(w, h)
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)

        if (
            lastPaddingLeft != paddingLeft ||
            lastPaddingTop != paddingTop ||
            lastPaddingRight != paddingRight ||
            lastPaddingBottom != paddingBottom
        ) {
            updateDrawingCache(width, height)

            lastPaddingLeft = paddingLeft
            lastPaddingTop = paddingTop
            lastPaddingRight = paddingRight
            lastPaddingBottom = paddingBottom
        }
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    /**
     * 计算
     */
    private fun updateDrawingCache(width: Int, height: Int) {
        updateGeometry(width, height)
        updateTextSize()
        updateTextBaseLine()
    }

    /**
     * 计算圆心，半径，RectF
     */
    private fun updateGeometry(w: Int, h: Int) {
        val contentWidth = (w - paddingLeft - paddingRight).toFloat().coerceAtLeast(0f)
        val contentHeight = (h - paddingTop - paddingBottom).toFloat().coerceAtLeast(0f)
        centerX = paddingLeft + contentWidth / 2
        centerY = paddingTop + contentHeight / 2
        radius = ((minOf(contentWidth, contentHeight) - paint.strokeWidth) / 2f).coerceAtLeast(0f)
        rectF = RectF(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
    }

    /**
     * 计算textSize
     */
    private fun updateTextSize() {
        if (isDependOnTextSize) return
        val availableRadius = (radius - paint.strokeWidth / 2 - textPadding - tickPadding - tickLength).coerceAtLeast(0f)
        textPaint.textSize = 1f * den
        val textWidth = textPaint.measureText("100%")
        val textHeight = textPaint.fontMetrics.descent - textPaint.fontMetrics.ascent
        val curRadius = hypot(textWidth / 2, textHeight / 2)
        val scale = if (curRadius > 0f) availableRadius / curRadius else 0f
        textPaint.textSize = floor(scale) * den
    }

    /**
     * 计算baseLine
     */
    private fun updateTextBaseLine() {
        baseLine = getBaseLine(centerY, textPaint.fontMetrics)
    }

    private fun isInCircle(x: Float, y: Float): Boolean {
        if (radius <= 0f) return false
        val distance = hypot(x - centerX, y - centerY)
        return abs(distance - radius) < TOUCH_WIDTH * den / 2
    }

    private fun calculateProgress(x: Float, y: Float) {
        if (!isInCircle(x, y) && !isDragging) return
        val dx = x - centerX
        val dy = y - centerY
        val standardAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val angle = (standardAngle + 90f + 360f) % 360f
        val progress = angle / 360f * 100f
        applyProgress(progress)
    }

    private fun measureCircle(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        if (isDependOnTextSize) {
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
        val fm = textPaint.fontMetrics
        val textHeight = fm.descent - fm.ascent
        val diameter = ceil(
            (
                    hypot(textWidth, textHeight) +
                            2f * textPadding +
                            2f * paint.strokeWidth + 2f * tickPadding + 2f * tickLength
                    ).toDouble()
        ).toInt()

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
        drawTicks(canvas)
    }

    private fun drawCircle(canvas: Canvas) {
        if (radius <= 0f) return
        val startAngle = -90f
        val sweepAngle = progress / 100f * 360f
        canvas.drawText(
            "${progress.toInt()}%",
            centerX,
            baseLine,
            textPaint
        )
        canvas.drawArc(rectF, startAngle, sweepAngle, false, paint)
    }

    private fun drawTicks(canvas: Canvas) {
        val angle = progress / 100f * 360f
        val textRadius = radius - paint.strokeWidth/2f - tickPadding - tickLength - textPadding
        val startX = centerX
        val startY = centerY - textRadius - tickLength - textPadding
        val stopY = startY + tickLength
        val stopX = centerX
        canvas.save()
        for (i in 0 until TOTAL_TICK_COUNT) {
            val curAngle = i * PER_TICK_ANGLE
            if (angle > curAngle) {
                canvas.drawLine(
                    startX,
                    startY,
                    stopX,
                    stopY,
                    tickPaint
                )
            } else {
                canvas.drawLine(
                    startX,
                    startY,
                    stopX,
                    stopY,
                    baseTickPaint
                )
            }
            canvas.rotate(PER_TICK_ANGLE.toFloat(),centerX,centerY)
        }
        canvas.restore()
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

    private fun applyProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    fun updateProgress(value: Float) {
        require(value.isFinite())
        progressAnimator?.cancel()
        progressAnimator = null
        applyProgress(value)
    }

    fun animateProgressTo(
        target: Float,
        durationMs: Long = 1000L
    ) {
        if (isDragging) return
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, target.coerceIn(0f, 100f)).apply {
            duration = durationMs.coerceAtLeast(1000L)
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                applyProgress(animatedValue)
            }
            start()
        }
    }

    fun animateProgress() {
        animateProgressTo(100f, 3000L)
    }
}