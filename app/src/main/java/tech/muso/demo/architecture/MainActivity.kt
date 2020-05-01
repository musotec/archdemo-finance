package tech.muso.demo.architecture

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import tech.muso.demo.theme.ThemeTestFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FrameLayout(this).apply {
            id = View.generateViewId() // new id for this layout
            // attach to activity
            setContentView(this, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            // simple fragment to test and make theme
            supportFragmentManager.beginTransaction().add(id, ThemeTestFragment()).commit()
        }
    }
}