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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import tech.muso.demo.architecture.BuildConfig
import tech.muso.demo.architecture.R
import tech.muso.demo.architecture.databinding.FragmentPinEntryBinding
import tech.muso.demo.architecture.ui.main.TouchLockSemaphore
import tech.muso.demo.architecture.viewmodels.AuthenticationViewModel
import tech.muso.demo.repos.AuthenticationRepository

/**
 * A Fragment to show a pin lock and entry when the app opens.
 *
 * This fragment takes a parameter to show compile time advantage achieved from design-by-contract.
 * Also it allows us to fix the bug with competing touch handlers for dragging the guideline and
 * the MainActivity's ViewPager2.
 *
 * Since the parameter is a required argument, it forces the preconditions to be met. Where passing
 * the arguments via [setArguments] would not. This is one step beyond the standard case, and has
 * the benefit of being able to send objects that are not Parcelable.
 */
class PinEntryFragment(private val touchLock: TouchLockSemaphore) : Fragment() {
    /**
     * Create a ViewModel that is tied to the Activity, so that it can be used across fragments,
     * but that takes in parameters (exampleArgPinLength) such that if two instances were needed
     * with different parameters they would be treated as different.
     */
    private val pinEntryViewModel: AuthenticationViewModel by activityViewModels {
        AuthenticationViewModel.Factory(4, requireActivity().application)
    }

    /**
     * We could instead just use retainInstance = true, which would allow us to circumvent any
     * Fragment recreation.
     *
     * However, this approach has the drawback of not being able to communicate the updated
     * parameter object [touchLock].
     *
     * Although this may seem to be the answer to some initial problems it is important to know the
     * draw backs. Naive implementations can result in following a path eventually leading to a
     * necessary refactoring of large sections of code or even worse band-aid hacks to fix issues.
     *
     * To achieve the same result would require the MainActivity to resend the parameters explicitly
     * to the Fragment. The Activity would then have knowledge of the code in the Fragment, and the
     * Fragment would be expecting that the parent Activity would always be sending it.
     *
     * This results in the code being very rigid and difficult to maintain in the future.
     * When developing in a team, understanding the impact of an "all knowing" codebase is critical.
     * When working on such a codebase, a developer unfamiliar with the code will certainly have
     * difficulty implementing their task.
     *
     * The SOLID principle breakages are "Single Responsibility" and "Liskov Substitution".
     *
     * The [Fragment.instantiate] docs suggest setting arguments via [Fragment.setArguments].
     * However, this only allows objects that are able to be put in a Bundle to be sent.
     * This also makes design-by-contract difficult, as setArguments() might not be called, despite
     * the fact that the parameters are essential; leaving no compile time protection.
     */
//    init {
//        retainInstance = true
//    }

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
                        MotionEvent.ACTION_DOWN -> touchLock.lock().run { isSelected = true }
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
                        MotionEvent.ACTION_UP -> touchLock.unlock().run { isSelected = false }
                        else -> false
                    }

                    // consume all touch events on our tiny handle.
                    return@setOnTouchListener true
                }
            }
        }

        // attach our ViewModel to the DataBinding class.
        binding.lifecycleOwner = viewLifecycleOwner // attach lifecycle owner to register draw callbacks
        binding.viewmodel = pinEntryViewModel // now attach ViewModel for bindings

        AuthenticationRepository.isAppUnlocked.observe(viewLifecycleOwner, Observer {
            pinEntryViewModel.clearPin()
        })

        return root
    }

}