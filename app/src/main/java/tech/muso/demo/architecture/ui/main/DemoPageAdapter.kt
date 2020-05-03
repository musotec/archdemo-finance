package tech.muso.demo.architecture.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import tech.muso.demo.architecture.ui.authentication.PinEntryFragment
import tech.muso.demo.architecture.ui.stocks.StockListFragment
import tech.muso.demo.theme.ThemeTestFragment

/**
 * A [FragmentPagerAdapter] that controls the display of a fragment for the current tab.
 */
class DemoPageAdapter(private val activity: FragmentActivity, fm: FragmentManager,
                      private val viewPagerScrollLock: TouchLockSemaphore)
    : FragmentStateAdapter(fm, activity.lifecycle) {

    /**
     * Factory to obtain a new fragment with a non-empty constructor.
     *
     * Although this is non-standard, and excessive in this basic example, the implementation is
     * both valuable and powerful for adherence to the Dependence Inversion and design-by-contract.
     *
     * The factory is owned by the page page adapter, which is tied to the parent Activity's
     * onCreate() lifecycle callback. This is FragmentFactory is best contained in this class, as
     * the ViewPager2 is owned by the Activity, and the locking mutex exists as a minimal interface
     * between the two. This keeps the business logic between reattaching the mutex contained within
     * the class that requires it in the first place.
     *
     * This way the MainActivity does not need to know _why_ the Adapter requires the mutex.
     * Since the Adapter is determining that it needs to pass it to the Fragments within it, the
     * MainActivity would need to know about what Fragments are in the adapter to explicitly
     * reattach the mutex.
     *
     * This is to achieve a MVP (Model-View-Presenter) pattern.
     *
     * With this Adapter class being the interface provided to the [TabLayoutMediator], which exists
     * as the controller/presenter in the middle.
     */
    private class FragmentFactoryImpl(val persistentTouchLock: TouchLockSemaphore) : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
            return when (className) {
                PinEntryFragment::class.java.name -> PinEntryFragment(
                    persistentTouchLock
                )
                else -> super.instantiate(classLoader, className)
            }
        }
    }

    // specify the fragment factory so that our activity can re-instantiate our PinEntryFragment
    // now that we have implemented the custom constructor
    init {
        fm.fragmentFactory = FragmentFactoryImpl(viewPagerScrollLock)
    }

    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return when(position) {
            0 -> PinEntryFragment(viewPagerScrollLock)
            1 -> StockListFragment()
            else -> ThemeTestFragment()
        }
    }


}