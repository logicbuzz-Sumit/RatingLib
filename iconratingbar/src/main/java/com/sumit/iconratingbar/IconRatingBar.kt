package com.sumit.iconratingbar
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources

import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class IconRatingBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var emptyDrawable: Drawable? = null
    private var filledDrawable: Drawable? = null
    private var halfDrawable: Drawable? = null

    private var starSizePx: Int = dp(55)
    private var starSpacingPx: Int = dp(6)

    var numStars: Int = 5
        set(value) {
            field = value.coerceAtLeast(1)
            buildStars()
            updateUI()
        }

    /** 1.0f = only full stars, 0.5f = half-star steps */
    var stepSize: Float = 1.0f

    /** allow user to set 0 by tapping/swiping far left */
    var allowZeroRating: Boolean = true

    var rating: Float = 0f
        set(value) {
            val min = if (allowZeroRating) 0f else 1f
            val newValue = value.coerceIn(min, numStars.toFloat())
            if (field == newValue) return
            field = newValue
            updateUI()
            onRatingChanged?.invoke(field)
        }

    var isIndicatorOnly: Boolean = false
        set(value) {
            field = value
            isEnabled = !value
        }

    var onRatingChanged: ((Float) -> Unit)? = null

    private val starBounds = ArrayList<Rect>()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var downX = 0f
    private var downY = 0f
    private var isDragging = false

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL

        context.theme.obtainStyledAttributes(attrs, R.styleable.IconRatingBar, 0, 0).apply {
            try {
                emptyDrawable = getDrawable(R.styleable.IconRatingBar_emptyStar)
                filledDrawable = getDrawable(R.styleable.IconRatingBar_filledStar)
                halfDrawable = getDrawable(R.styleable.IconRatingBar_halfStar)
                starSizePx = getDimensionPixelSize(R.styleable.IconRatingBar_starSize, starSizePx)
                starSpacingPx =
                    getDimensionPixelSize(R.styleable.IconRatingBar_starSpacing, starSpacingPx)
            } finally {
                recycle()
            }
        }

        if (emptyDrawable == null) {
            emptyDrawable =
                AppCompatResources.getDrawable(context, android.R.drawable.btn_star_big_off)
        }
        if (filledDrawable == null) {
            filledDrawable =
                AppCompatResources.getDrawable(context, android.R.drawable.btn_star_big_on)
        }

        buildStars()
        updateUI()
    }

    fun setIcons(
        @DrawableRes empty: Int,
        @DrawableRes filled: Int,
        @DrawableRes half: Int? = null
    ) {
        emptyDrawable = AppCompatResources.getDrawable(context, empty)
        filledDrawable = AppCompatResources.getDrawable(context, filled)
        halfDrawable = half?.let { AppCompatResources.getDrawable(context, it) }
        updateUI()
    }

    private fun buildStars() {
        removeAllViews()
        starBounds.clear()

        for (i in 1..numStars) {
            val iv = ImageView(context).apply {
                layoutParams = LayoutParams(starSizePx, starSizePx).apply {
                    if (i != 1) marginStart = starSpacingPx
                }
                // IMPORTANT: don't let child consume touch; parent handles swipe + tap
                isClickable = false
                isFocusable = false
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
            addView(iv)
            starBounds.add(Rect())
        }

        // rebuild bounds after layout
        post { updateStarBounds() }
    }

    private fun updateStarBounds() {
        for (i in 0 until childCount) {
            val v = getChildAt(i)
            starBounds[i].set(v.left, v.top, v.right, v.bottom)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        post { updateStarBounds() }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isIndicatorOnly) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                isDragging = false
                parent?.requestDisallowInterceptTouchEvent(true)
                // consume so we can detect click/swipe
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = abs(event.x - downX)
                val dy = abs(event.y - downY)
                if (!isDragging && (dx > touchSlop || dy > touchSlop)) {
                    isDragging = true
                }
                // live update during drag
                if (isDragging) {
                    setRatingFromTouch(event.x)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                // If it wasn't a drag -> treat as click/tap
                if (!isDragging) {
                    setRatingFromTouch(event.x)
                } else {
                    // final update on release
                    setRatingFromTouch(event.x)
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                return false
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    private fun setRatingFromTouch(x: Float) {
        // handle far left = 0 (optional)
        if (allowZeroRating && x <= 0f) {
            rating = 0f
            return
        }

        // Ensure bounds are available
        if (starBounds.isEmpty() || starBounds.size != numStars) {
            // fallback approximate
            val approx = (x / width.coerceAtLeast(1)) * numStars
            rating = snapToStep(approx, stepSize)
            return
        }

        // Find which star we are in
        var result = if (allowZeroRating) 0f else 1f

        for (i in 0 until numStars) {
            val b = starBounds[i]
            // expand a little for easier touch
            val left = b.left - dp(2)
            val right = b.right + dp(2)

            if (x < left) {
                // before this star
                break
            }

            if (x in left.toFloat()..right.toFloat()) {
                // inside star i
                val localX = (x - b.left).coerceIn(0f, (b.width()).toFloat())
                val fraction = if (b.width() == 0) 1f else localX / b.width().toFloat()

                result = when {
                    stepSize <= 0.5f -> (i + if (fraction < 0.5f) 0.5f else 1f).toFloat()
                    else -> (i + 1).toFloat() // full star steps
                }
                rating = snapToStep(result, stepSize)
                return
            } else {
                // passed this star completely
                result = (i + 1).toFloat()
            }
        }

        // after last star
        rating = snapToStep(result, stepSize)
    }

    private fun snapToStep(value: Float, step: Float): Float {
        if (step <= 0f) return value
        return (round(value / step) * step)
    }

    private fun updateUI() {
        val full = rating.toInt()
        val hasHalf = (rating - full) >= 0.5f

        for (index in 0 until childCount) {
            val starIndex = index + 1
            val iv = getChildAt(index) as ImageView

            val drawable = when {
                starIndex <= full -> filledDrawable
                starIndex == full + 1 && hasHalf && halfDrawable != null -> halfDrawable
                else -> emptyDrawable
            }
            iv.setImageDrawable(drawable)
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()
}