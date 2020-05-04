package tech.muso.demo.architecture.view

import android.content.Context
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import androidx.annotation.AnyThread
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter

/**
 * Apply a color to a list of character positions.
 */
fun CharSequence.applyColorSpanToIndexList(@ColorInt color: Int, indexList: List<Int>?): Spannable {
    val builder = if (this is SpannableStringBuilder) this else SpannableStringBuilder(this)
    indexList?.forEach { i ->
        if (i >= this.length) return@forEach    // exit if out of position.
        builder.setSpan(
            ForegroundColorSpan(color), i, i+1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return builder
}

/**
 * Bind the total pin length to the view to know how long the expected pin is.
 */
@BindingAdapter("app:pinViewState")
fun bindPinViewState(view: HiddenPinView, pinViewState: HiddenPinView.PinViewState) {
    view.setPinViewState(pinViewState)
}


@BindingAdapter("app:pinValidityState")
fun bindPinValidityState(view: HiddenPinView, pinValidityState: HiddenPinView.PinValidityState) {
    view.setPinValidityState(pinValidityState)
}

/**
 * Create a view that will display a PIN blocked out with triangles.
 */
class HiddenPinView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    /**
     * Hold an immutable state of the pin validity.
     * Empty constructor defaults to no state.
     * @param showHint if false, list data should not be respected.
     * @param correctList the list of correct indices in the pin.
     * @param incorrectList the list of incorrect indices in the pin.
     */
    data class PinValidityState(val showHint: Boolean = false,
                                val correctList: List<Int> = listOf(),
                                val incorrectList: List<Int> = listOf())

    /**
     * Separate holder for a tuple of two pins for current length vs total length.
     */
    data class PinViewState(val currentPinLength: Int, val totalPinLength: Int)

    companion object {
        private const val DOWN_EMPTY: Char = '▽'
        private const val UP_EMPTY: Char = '△'
        private const val DOWN_FILLED: Char = '▼'
        private const val UP_FILLED: Char = '▲'

        /**
         * Static method for generating the characters in the hidden pin view.
         */
        @JvmStatic
        private fun generatePinString(currentPinLength: Int, totalPinLength: Int): String {
            // create the character list
            val charList = (0 until totalPinLength).map { index ->
                if (index >= currentPinLength) {
                    // use empty if out of range
                    if (index % 2 == 0) UP_EMPTY else DOWN_EMPTY
                } else {
                    // and filled for in range
                    if (index % 2 == 0) UP_FILLED else DOWN_FILLED
                }
            }

            return charList.joinToString(separator = "")
        }
    }

    //
    // Private variables required for internal functionality.
    //
    private var originalText: CharSequence? = null
    private var correctList: List<Int>? = null
    private var incorrectList: List<Int>? = null
    private var totalPinLength: Int = 0
    private var currentPinLength: Int = 0

    //
    // Public variables exposed for apis.
    //

    /**
     * Set the [PinViewState].
     */
    @AnyThread
    fun setPinViewState(pinViewState: PinViewState) {
        totalPinLength = pinViewState.totalPinLength
        currentPinLength = pinViewState.currentPinLength
        postInvalidate()
    }

    /**
     * Set the [PinValidityState].
     */
    @AnyThread
    internal fun setPinValidityState(pinValidityState: PinValidityState) {
        if(pinValidityState.showHint) {
            correctList = pinValidityState.correctList
            incorrectList = pinValidityState.incorrectList
        } else {
            correctList = null
            incorrectList = null
        }
        postInvalidate()
    }

    // could overload to expose the raw value, but this is beyond the scope of the current use case
//    override fun getText(): CharSequence? {
//        return originalText
//    }

    /**
     * Update the text when set to colorize the indices of the backing text.
     * Indicating correct values would never actually be done in production, just for our demo.
     */
    override fun setText(text: CharSequence?, type: BufferType?) {
        originalText = text
        // set display text to be a obfuscated pin entry
        super.setText(
            generatePinString(currentPinLength, totalPinLength)
            .applyColorSpanToIndexList(Color.RED, incorrectList)
            .applyColorSpanToIndexList(Color.GREEN, correctList), type)
    }

    /**
     * Override the invalidate method, as we are intercepting the [setText] method in order to
     * achieve the highlighting with [Spannable] objects.
     *
     * Which we need to do because we actually change the text.
     * Otherwise the normal [onDraw] will be re-called with the old spannable text.
     */
    override fun invalidate() {
        text = originalText
        super.invalidate()
    }
}