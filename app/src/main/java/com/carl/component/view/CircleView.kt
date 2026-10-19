package com.carl.component.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import com.carl.component.R
import kotlin.math.roundToInt

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density

    // wrap_content 时的默认内容直径：120dp
    private val defaultDiameter = (120f * density).roundToInt()

    private val paintOut = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
    }

    private val paintIn = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.TRANSPARENT
    }

    private var ringWidthPx = 12f * density

    init {
        val ta = context.obtainStyledAttributes(
            attrs,
            R.styleable.CircleView,
            defStyleAttr,
            0
        )

        try {
            paintOut.color = ta.getColor(
                R.styleable.CircleView_outColor,
                Color.BLUE
            )

            paintIn.color = ta.getColor(
                R.styleable.CircleView_inColor,
                Color.TRANSPARENT
            )

            ringWidthPx = ta.getDimension(
                R.styleable.CircleView_ringWidth,
                12f * density
            ).coerceAtLeast(0f)
        } finally {
            ta.recycle()
        }
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val desiredWidth = defaultDiameter + paddingLeft + paddingRight
        val desiredHeight = defaultDiameter + paddingTop + paddingBottom

        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val contentWidth = width - paddingLeft - paddingRight
        val contentHeight = height - paddingTop - paddingBottom

        val diameter = minOf(contentWidth, contentHeight).toFloat()

        if (diameter <= 0f) return

        val centerX = paddingLeft + contentWidth / 2f
        val centerY = paddingTop + contentHeight / 2f

        val outerRadius = diameter / 2f

        // 防止圆环宽度超过有效范围
        val stroke = ringWidthPx.coerceIn(0f, outerRadius)

        // 先画内部填充
        canvas.drawCircle(
            centerX,
            centerY,
            outerRadius,
            paintIn
        )

        // 再画外部圆环
        if (stroke > 0f) {
            paintOut.strokeWidth = stroke

            canvas.drawCircle(
                centerX,
                centerY,
                outerRadius - stroke / 2f,
                paintOut
            )
        }
    }

    fun setColors(
        @ColorInt outColor: Int,
        @ColorInt inColor: Int
    ) {
        paintOut.color = outColor
        paintIn.color = inColor
        invalidate()
    }

    fun setRingWidthDp(dp: Float) {
        require(dp >= 0f)
        ringWidthPx = dp * density
        invalidate()
    }
}