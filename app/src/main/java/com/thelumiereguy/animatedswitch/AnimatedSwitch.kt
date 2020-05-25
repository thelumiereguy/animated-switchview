package com.thelumiereguy.animatedswitch

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.transition.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.graphics.toRectF
import androidx.core.view.updateLayoutParams
import kotlin.math.roundToInt


class AnimatedSwitch @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), View.OnClickListener {

    private val horizontalPadding by lazy {
        8 * context.resources.displayMetrics.density.roundToInt()
    }

    private val onColour = Color.GREEN
    private val offColour = Color.RED

    private val transitionDuration by lazy {
        context.resources.getInteger(android.R.integer.config_mediumAnimTime).toLong()
    }


    //Measuring width of text with more characters
    private val textViewWidth by lazy {
        (textView.paint.measureText(SWITCHSTATUS.OFF.name) + (horizontalPadding * 2)).roundToInt()
    }

    private val transition: Transition by lazy {
        TransitionSet().apply {
            addTransition(ChangeBounds().also { it.duration = transitionDuration })
            addTransition(ChangeTransform().also { it.duration = transitionDuration })
        }
    }

    var onCheckChangedListener: ((AnimatedSwitch, Boolean) -> Unit)? = null


    //Size of switch thumb
    private
    val thumbSize = 24F.toDp(context).roundToInt()


    private val textPaddingBottom = 4

    //Default Status
    private var currentStatus: SWITCHSTATUS = SWITCHSTATUS.ON

    private val textView by lazy {
        AppCompatTextView(context)
    }

    private val thumb by lazy {
        AppCompatImageView(context)
    }

    //bounds for drawing background rounded Rectangle
    private val backgroundRect by lazy {
        Rect(
            0,
            0,
            measuredWidth,
            measuredHeight
        )
    }

    /**
     * paint for drawing background
     */
    private val backgroundPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }
    }

    /**
     * Add Views and their layout Params
     */
    init {
        setWillNotDraw(false)
        addView(textView)
        addView(thumb)
        backgroundPaint.apply {
            color = if (currentStatus == SWITCHSTATUS.ON) {
                onColour
            } else {
                offColour
            }
        }
        isClickable = true
        isFocusable = true
        initText()
        initThumb()
        setOnClickListener(this)
    }


    private fun initText() {
        textView.apply {
            text = currentStatus.name
            textSize = 14F
            gravity = Gravity.CENTER
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(Color.WHITE)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(horizontalPadding, 0, horizontalPadding, textPaddingBottom)
            updateLayoutParams<LayoutParams> {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                height = thumbSize
                width = textViewWidth
            }
        }
    }

    private fun initThumb() {
        thumb.apply {
            setBackgroundResource(R.drawable.custom_switch_thumb)
            updateLayoutParams<LayoutParams> {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                height = thumbSize
                width = thumbSize
            }
        }
    }

    /**
     * Width = TextWidth + DrawableWidth
     * Height = DrawableHeight
     */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(
            textViewWidth + thumbSize,
            thumbSize + textPaddingBottom
        )
    }

    /**
     * Draw rounded rectangle in the background
     */
    override fun onDraw(canvas: Canvas?) {
        canvas?.let {
            it.drawRoundRect(
                backgroundRect.toRectF(),
                measuredHeight.toFloat() / 2,
                measuredHeight.toFloat() / 2,
                backgroundPaint
            )
        }
        super.onDraw(canvas)
    }


    override fun onClick(v: View?) {
        toggle()
        onCheckChangedListener?.invoke(this, currentStatus == SWITCHSTATUS.ON)
    }

    /**
     * Toggle status
     * Start Transitions
     * Flip Views
     * Animate Color
     */
    private fun toggle() {
        currentStatus = currentStatus.flip()
        textView.text = currentStatus.name
        TransitionManager.beginDelayedTransition(this, transition)
        toggleBackgroundColour()
        flipParams()
    }

    private fun toggleBackgroundColour() {
        if (currentStatus == SWITCHSTATUS.ON) {
            animateTo(offColour, onColour)
        } else {
            animateTo(onColour, offColour)
        }
    }

    private fun animateTo(colorFrom: Int, colorTo: Int) {
        val colorAnimation: ValueAnimator =
            ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
        colorAnimation.duration = transitionDuration
        colorAnimation.addUpdateListener { animator ->
            backgroundPaint.apply {
                color = animator.animatedValue as Int
            }
        }
        colorAnimation.start()
    }

    private fun flipParams() {
        if (currentStatus == SWITCHSTATUS.ON) {
            textView.updateLayoutParams<LayoutParams> {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
            thumb.updateLayoutParams<LayoutParams> {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
        } else {
            textView.updateLayoutParams<LayoutParams> {
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }
            thumb.updateLayoutParams<LayoutParams> {
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
            }
        }
    }
}

sealed class SWITCHSTATUS(val name: String) {
    object ON : SWITCHSTATUS("ON")
    object OFF : SWITCHSTATUS("OFF")

    fun flip(): SWITCHSTATUS {
        return if (this is ON) OFF else ON
    }
}


fun Float.toDp(context: Context): Float {
    return context.resources.displayMetrics.density * this
}