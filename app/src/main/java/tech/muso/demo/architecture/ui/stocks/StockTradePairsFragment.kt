package tech.muso.demo.architecture.ui.stocks

import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import kotlinx.coroutines.FlowPreview
import tech.muso.demo.architecture.DemoArchitectureApplication
import tech.muso.demo.architecture.databinding.ViewOrderPairsTradeBinding
import tech.muso.demo.architecture.viewmodels.stocks.StocksTradePairsViewModel

/**
 * Basic DialogFragment with temporary custom view for viewing the Stock object on click.
 */
class StockTradePairsFragment : Fragment() {

    @FlowPreview
    private val viewModel: StocksTradePairsViewModel by viewModels {
        DemoArchitectureApplication.getInstance().getStockViewModelProvider(requireContext(), this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = ViewOrderPairsTradeBinding.inflate(inflater, container, false)
        context ?: return binding.root

        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        // quick orientation hack to lazily avoid two layouts
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.chartVisibility.value = View.GONE
        }

//        binding.firstStock = viewModel.first
//        binding.secondStock = viewModel.second

//        for(i in 0..50) {
//            addStock(
//                // need to do a deep copy because we're being lazy about creating objects
//                stock.deepCopy().also {
//                    generateNextTick()
//                }
//            )
//        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

}