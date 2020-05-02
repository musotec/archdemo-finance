package tech.muso.demo.architecture.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import tech.muso.demo.architecture.databinding.ViewKeyPadBinding

/**
 * A custom view for the Number Pad entry view
 */
class NumberPadEntryView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    init {
        // inflate
        val binding = ViewKeyPadBinding.inflate(LayoutInflater.from(context), this as FrameLayout, true)
    }
}