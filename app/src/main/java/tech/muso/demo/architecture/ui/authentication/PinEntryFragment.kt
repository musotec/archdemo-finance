package tech.muso.demo.architecture.ui.authentication

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import tech.muso.demo.architecture.BuildConfig
import tech.muso.demo.architecture.R
import tech.muso.demo.architecture.databinding.FragmentPinEntryBinding

class PinEntryFragment() : Fragment() {

    // View accessibility is excessive for this demo OnTouchListener
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // inflate our layout using the DataBindingUtil
        val binding: FragmentPinEntryBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_pin_entry, container, false)
        val root = binding.root

        // perform debug build flavor demonstration code
        if (BuildConfig.DEBUG) {
            // NOTE: Lazily create anonymous touch listener to demonstrate ConstraintLayout benefits
            //   ideally, this would be its own class if we wanted this in a production build.
            val guideline = binding.guideline
            binding.handle.apply {
                setOnTouchListener { v, event ->
                    when (event.action) {
                        // NOTE: the ImageView's selected state controls the drawable color
                        //   in the color_selector_on_primary.xml color selector
                        MotionEvent.ACTION_DOWN -> isSelected = true
                        MotionEvent.ACTION_MOVE -> {
                            // update the ConstraintLayout Guide position to where we are touching
                            val percentHeight: Float =
                                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                                    event.rawY / resources.displayMetrics.heightPixels
                                } else {
                                    event.rawX / root.width
                                }
                            guideline.setGuidelinePercent(percentHeight)
                        }
                        // ACTION_CANCEL and ACTION_UP are both required to handle drag off case
                        MotionEvent.ACTION_CANCEL,
                        MotionEvent.ACTION_UP -> isSelected = false
                        else -> false
                    }

                    // consume all touch events on our tiny handle.
                    return@setOnTouchListener true
                }
            }
        }

        return root
    }

}