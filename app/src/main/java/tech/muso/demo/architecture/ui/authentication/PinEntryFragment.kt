package tech.muso.demo.architecture.ui.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import tech.muso.demo.architecture.R

class PinEntryFragment() : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // simple bind to view to design
        return inflater.inflate(R.layout.fragment_pin_entry, container, true)
    }

}