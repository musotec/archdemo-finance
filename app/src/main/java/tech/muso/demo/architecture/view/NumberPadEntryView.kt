package tech.muso.demo.architecture.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import tech.muso.demo.architecture.R
import tech.muso.demo.architecture.databinding.ViewKeyPadBinding
import java.lang.IllegalStateException

@BindingAdapter("app:numberPadListener")
fun setNumberPadListener(view: NumberPadEntryView?, listener: NumberPadEntryView.NumberPadOnClickListener?) {
    view?.onNumberPadOnClickListener = listener
}

/**
 * A custom view for the Number Pad view, with DataBinding to attach onClickListeners to children.
 */
class NumberPadEntryView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var onNumberPadOnClickListener: NumberPadOnClickListener? = null

    interface NumberPadOnClickListener {
        /** Update on key press directly */
        fun onNumKeyPress(number: Int)
        fun onBackKeyPress()

        /** Alternative approach to connect directly to TextView */
        fun connectToTextView(): TextView? {
            return null
        }
    }

    /**
     * In this init block we could read any attributes, but we don't have any defined that aren't
     * automatically bound via BindingAdapters.
     *
     * We would really only want to read in the initial attributes manually here if the initial
     * value specified in the XML should be cached for whatever reason.
     * Otherwise, the BindingAdapter approach is less verbose and more robust.
     */
    init {
        val binding = ViewKeyPadBinding.inflate(LayoutInflater.from(context), this as FrameLayout, true)

        // listeners are declared inline so that the knowledge of the NumberPadOnClickListener
        // Class does not matter. Otherwise, the objects would have to be declared above the init
        // code block in order to compile.
        binding.onClickListener = OnClickListener { view ->
            val numberValue: Int = when (view.id) {
                R.id.pin_key_0 -> 0
                R.id.pin_key_1 -> 1
                R.id.pin_key_2 -> 2
                R.id.pin_key_3 -> 3
                R.id.pin_key_4 -> 4
                R.id.pin_key_5 -> 5
                R.id.pin_key_6 -> 6
                R.id.pin_key_7 -> 7
                R.id.pin_key_8 -> 8
                R.id.pin_key_9 -> 9
                // below case should never be thrown unless the linked xml is broken
                else -> throw IllegalStateException("pressKeyPad was called from an invalid view id!")
            }

            onNumberPadOnClickListener?.onNumKeyPress(numberValue)
        }

        binding.onBackspaceClickListener = OnClickListener {
            onNumberPadOnClickListener?.onBackKeyPress()
        }
    }
}