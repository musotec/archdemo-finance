package tech.muso.demo.architecture.ui.stocks

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import tech.muso.demo.architecture.R
import tech.muso.demo.common.entity.Stock

/**
 * Basic DialogFragment with temporary custom view for viewing the Stock object on click.
 */
class ViewStockDialogFragment : DialogFragment() {

    companion object {
        fun newInstance(stock: Stock): ViewStockDialogFragment {
            val args = Bundle()
            args.putString("title", stock.name)
            val fragment = ViewStockDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireActivity()).apply {
            val inflater = requireActivity().layoutInflater
            val view = inflater.inflate(R.layout.view_order_pairs_trade, null)
            setView(view)
            setMessage(arguments?.getString("title"))
            setCancelable(true)
        }.create()
    }
}