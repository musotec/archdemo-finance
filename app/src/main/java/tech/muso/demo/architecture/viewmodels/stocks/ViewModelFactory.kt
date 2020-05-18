package tech.muso.demo.architecture.viewmodels.stocks

import androidx.lifecycle.*
import kotlinx.coroutines.*
import tech.muso.demo.repos.StockDataRepository

// Define some aliases for readability
typealias StockFilter = Int
const val NoFilter: StockFilter = 0

/**
 * Factory class, which serves up the repository singleton to the ViewModels that are created.
 *
 * This way we avoid worrying about multiple instances of the Stocks repository across fragments.
 *
 * e.g. if there were multiple collections of stocks, one for watch list, one for long positions,
 * and a third for short positions; we wouldn't need to manage three different repositories for
 * accessing the same data.
 *
 * This also allows us to determine what type of ViewModel we will need to generate based on the
 * class name.
 */
@Suppress("UNCHECKED_CAST")
class ViewModelFactory(
    private val repository: StockDataRepository
) : ViewModelProvider.NewInstanceFactory() {

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass.name) {
            StocksViewModel::class.java.name ->
                StocksViewModel(
                    repository
                )
            else ->
                StocksTradePairsViewModel(
                    repository
                )
        } as T
    }
}