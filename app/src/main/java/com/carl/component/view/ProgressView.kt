package com.carl.component.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View

import com.carl.component.R
import kotlin.math.roundToInt


class ProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
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
     *
     */
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f * den
        strokeCap = Paint.Cap.ROUND
    }

    init {
        val ta = context.obtainStyledAttributes(attrs, R.styleable.ProgressView, defStyleAttr, 0)
        try {
            mode = ta.getInt(R.styleable.ProgressView_mode, 0)
            paint.color = ta.getColor(R.styleable.ProgressView_color, Color.BLUE)
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

    private fun measureCircle(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val desiredWidth = maxOf(
            defaultDiameter + paddingLeft + paddingRight,
            suggestedMinimumWidth
        )
        Log.d("jqhTest","suggestedMinimumWidth = $suggestedMinimumWidth,desiredWidth = $desiredWidth")
        val desiredHeight = maxOf(
            defaultDiameter + paddingTop + paddingBottom,
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

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val diameter = minOf(contentWidth, contentHeight) -
                paint.strokeWidth

        if (diameter <= 0f) return
        val radius = diameter / 2f
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

    }

    fun updateProgress(value: Float) {
        progress = value.coerceIn(0f, 100f)
        invalidate()
    }

    fun animateProgressTo(
        target: Float,
        durationMs:Long = 1000L
    ){
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(progress, target.coerceIn(0f, 100f)).apply {
            duration = durationMs
            addUpdateListener { animator ->
                val animatedValue = animator.animatedValue as Float
                updateProgress(animatedValue)
            }
            start()
        }
    }
    fun animateProgress(){
        animateProgressTo(100f,3000L)
    }
}