package tech.muso.demo.architecture.ui.main

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import tech.muso.demo.architecture.ui.stocks.StockListFragment
import tech.muso.demo.theme.ThemeTestFragment

class DemoPageAdapter(private val activity: FragmentActivity, fm: FragmentManager)
    : FragmentStateAdapter(fm, activity.lifecycle) {

    override fun getItemCount(): Int {
        return 2
    }

    override fun createFragment(position: Int): Fragment {
        if (position == 0) {
            return StockListFragment()
        } else return ThemeTestFragment()
    }


}