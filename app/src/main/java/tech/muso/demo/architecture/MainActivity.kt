package tech.muso.demo.architecture

// use kotlin android extensions to do findViewById() once, and cache results for us.
// this also handles loading in all the imports for the view classes
import kotlinx.android.synthetic.main.activity_main.*

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import tech.muso.demo.architecture.ui.main.DemoPageAdapter
import tech.muso.demo.architecture.ui.main.TouchLockSemaphore
import tech.muso.demo.repos.AuthenticationRepository

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val demoPageAdapter = DemoPageAdapter(this, supportFragmentManager,
            // Demonstration of Observer pattern with typical Java Style interface callbacks.
            TouchLockSemaphore(object : TouchLockSemaphore.Listener {
                override fun onLockChanged(isLocked: Boolean) {
                    // enable viewpager swipe if not locked.
                    view_pager.isUserInputEnabled = !isLocked
                }
            })
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_pager.adapter = demoPageAdapter

        // use mediator to connect viewpager and our empty TabLayout in XML
        val mediator: TabLayoutMediator = TabLayoutMediator(tabs, view_pager) { tab, position ->
            // onConfigureTab
            demoPageAdapter.link(tab, position)

            // special case to update our lock icon,
            // implementation is poor; using comparison to zero index (magic number)
            if (position == 0) {
                AuthenticationRepository.isAppUnlocked.observe(this, Observer {
                    val lockIconResource = if (it) R.drawable.ic_baseline_lock_open_24 else R.drawable.ic_baseline_lock_24
                    tab.icon = getDrawable(lockIconResource)
                })
            }
        }.apply { attach() } // attach() call once set up.

        // null/false/true boolean to avoid recalling code below when already unlocked
        var prevUnlockState: Boolean? = null

        // observe our authentication state, and when we unlock, move to the stocks tab.
        AuthenticationRepository.isAppUnlocked.observe(this,
            Observer<Boolean> { isUnlocked ->
                if (prevUnlockState == false && isUnlocked) {
                    Snackbar.make(view_pager, "PIN ENTRY SUCCESS", Snackbar.LENGTH_SHORT).show()
                    view_pager.setCurrentItem(1, true)
                }
                if (prevUnlockState == true && isUnlocked == false) {
                    Snackbar.make(view_pager, "LOCKED AFTER 60 SECONDS", Snackbar.LENGTH_SHORT).show()
                    view_pager.setCurrentItem(0, true)
                }

                prevUnlockState = isUnlocked
            }
        )
    }
}