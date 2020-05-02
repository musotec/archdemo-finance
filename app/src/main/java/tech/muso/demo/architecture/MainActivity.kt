package tech.muso.demo.architecture

// use kotlin android extensions to do findViewById() once, and cache results for us.
// this also handles loading in all the imports for the view classes
import kotlinx.android.synthetic.main.activity_main.*

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayoutMediator
import tech.muso.demo.architecture.ui.main.DemoPageAdapter

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val demoPageAdapter = DemoPageAdapter(this, supportFragmentManager)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        view_pager.adapter = demoPageAdapter
        // fixme: update to link to page adapter; make less hard coded
        val mediator: TabLayoutMediator = TabLayoutMediator(tabs, view_pager) { tab, position ->
            // onConfigureTab
            if (position == 0) tab.text = "pin" else tab.text = "theme"
        }.apply { attach() } // attach() call once set up.
    }
}