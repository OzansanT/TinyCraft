package com.tinycraft.nativeandroid

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

class FlowLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    var horizontalGapPx: Int = 0
    var verticalGapPx: Int = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val maxWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        var lineWidth = 0
        var lineHeight = 0
        var totalHeight = 0
        var usedWidth = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
            val lp = child.layoutParams as MarginLayoutParams
            val childW = child.measuredWidth + lp.leftMargin + lp.rightMargin
            val childH = child.measuredHeight + lp.topMargin + lp.bottomMargin

            if (lineWidth > 0 && lineWidth + horizontalGapPx + childW > maxWidth) {
                totalHeight += lineHeight + verticalGapPx
                usedWidth = maxOf(usedWidth, lineWidth)
                lineWidth = childW
                lineHeight = childH
            } else {
                if (lineWidth > 0) lineWidth += horizontalGapPx
                lineWidth += childW
                lineHeight = maxOf(lineHeight, childH)
            }
        }

        totalHeight += lineHeight
        usedWidth = maxOf(usedWidth, lineWidth)

        val measuredW = resolveSize(usedWidth + paddingLeft + paddingRight, widthMeasureSpec)
        val measuredH = resolveSize(totalHeight + paddingTop + paddingBottom, heightMeasureSpec)
        setMeasuredDimension(measuredW, measuredH)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val maxWidth = r - l - paddingLeft - paddingRight
        var x = paddingLeft
        var y = paddingTop
        var lineHeight = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility == View.GONE) continue
            val lp = child.layoutParams as MarginLayoutParams
            val childW = child.measuredWidth
            val childH = child.measuredHeight
            val occupiedW = lp.leftMargin + childW + lp.rightMargin

            if (x > paddingLeft && x + occupiedW > paddingLeft + maxWidth) {
                x = paddingLeft
                y += lineHeight + verticalGapPx
                lineHeight = 0
            }

            val left = x + lp.leftMargin
            val top = y + lp.topMargin
            child.layout(left, top, left + childW, top + childH)
            x += occupiedW + horizontalGapPx
            lineHeight = maxOf(lineHeight, lp.topMargin + childH + lp.bottomMargin)
        }
    }

    override fun generateDefaultLayoutParams(): LayoutParams =
        MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams =
        MarginLayoutParams(context, attrs)

    override fun generateLayoutParams(p: LayoutParams?): LayoutParams =
        MarginLayoutParams(p)

    override fun checkLayoutParams(p: LayoutParams?): Boolean = p is MarginLayoutParams
}
