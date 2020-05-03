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
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.BindingAdapter
import tech.muso.demo.architecture.viewmodels.PinObserveInterface
import java.lang.ref.WeakReference

/**
 * Apply a color to a list of character positions.
 */
fun CharSequence.applyColorSpanToIndexList(@ColorInt color: Int, indexList: List<Int>?): Spannable? {
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
@BindingAdapter("app:pinLength")
fun bindPinLength(view: HiddenPinView, pinLength: Int) {
    view.pinLength = pinLength
}

/**
 * A binding adapter that does a potentially dangerous case by passing the View on binding.
 * This is only "safe" because we have forced a WeakReference to be passed, hoping that the object
 * it holds does not get a strong reference down the line. This will be refactored.
 */
@BindingAdapter("app:pinObserver")
fun bindPinObserver(textView: HiddenPinView, pinObserverInterface: PinObserveInterface) {
    // note a WeakReference is used, as we do not wish to induce a memory leak with a View reference
    pinObserverInterface.invoke(WeakReference(textView))
}

/**
 * Create a view that will display a PIN blocked out with triangles.
 */
class HiddenPinView(context: Context, attrs: AttributeSet) : AppCompatTextView(context, attrs) {

    companion object {
        private const val DOWN_EMPTY: Char = '▽'
        private const val UP_EMPTY: Char = '△'
        private const val DOWN_FILLED: Char = '▼'
        private const val UP_FILLED: Char = '▲'

        /**
         * Static method for converting the text in the CharSequence.
         * We will remove this when we remove the need to pass a clear text value to this view.
         * This will be part of the refactoring.
         */
        @JvmStatic
        fun createEncodedPinString(clearTextPin: CharSequence?, totalPinLength: Int): String? {
            if (clearTextPin == null) return null

            // create the character list
            val charList = (0 until totalPinLength).mapIndexed { index: Int, value: Int ->
                if (index >= clearTextPin.length) {
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
    // Private variables required for the correctness hint/indicator functionality.
    //
    private var originalText: CharSequence? = null
    private var correctList: List<Int>? = null
    private var incorrectList: List<Int>? = null

    //
    // Public variables exposed for apis.
    //

    /**
     * The total length of the pin. Used to create empty triangles to indicate needed presses.
     *
     * When updated, the pin view will be invalidated.
     */
    var pinLength: Int = 0
        @AnyThread
        set(value) {
            field = value
            postInvalidate()    // postInvalidate() is used to ensure updatable from any thread
        }

    // could overload to expose the raw value, but this is beyond the scope of the current use case
//    override fun getText(): CharSequence? {
//        return originalText
//    }

    /**
     * Set the list of indices in the pin's backing Character array to mark visibly as correct.
     * @param list of indices that will be highlighted in green.
     */
    @UiThread
    fun setCorrectIndexList(list: List<Int>) {
        correctList = list
        invalidate()
    }

    /**
     * Set the list of indices in the pin's backing character array to mark visibly as incorrect.
     * @param list of indices that will be highlighted in red.
     */
    @UiThread
    fun setIncorrectIndexList(list: List<Int>) {
        incorrectList = list
        invalidate()
    }

    /**
     * Clears all highlighting on the hidden pin.
     */
    @UiThread
    fun clearPinValidityLists() {
        correctList = null
        incorrectList = null
        invalidate()
    }

    /**
     * Update the text when set to colorize the indices of the backing text.
     * Indicating correct values would never actually be done in production, just for our demo.
     */
    override fun setText(text: CharSequence?, type: BufferType?) {
        originalText = text
        // set display text to be a obfuscated pin entry
        super.setText(
            createEncodedPinString(text, pinLength)
            ?.applyColorSpanToIndexList(Color.RED, incorrectList)
            ?.applyColorSpanToIndexList(Color.GREEN, correctList), type)
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