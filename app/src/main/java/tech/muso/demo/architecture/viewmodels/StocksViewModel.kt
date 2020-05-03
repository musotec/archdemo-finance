package tech.muso.demo.architecture.viewmodels

import android.util.Log
import androidx.lifecycle.*
import kotlinx.coroutines.*
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.repos.StockDataRepository

// Define some aliases for readability
typealias StockFilter = Int
const val NoFilter: StockFilter = 0

@ExperimentalCoroutinesApi
@FlowPreview
class StocksViewModel internal constructor(
    private val repository: StockDataRepository
) : ViewModel() {

    private val _loadingState = MutableLiveData<Boolean>(false)
    /**
     * True if we are currently loading data
     */
    val loadingState: LiveData<Boolean>
        get() = _loadingState

    private val _snackbar = MutableLiveData<String?>(null)
    /**
     * Requested snackbar display message.
     *
     * Call [resetSnackbarState] after handling the message.
     */
    val snackbar: LiveData<String?>
        get() = _snackbar

    /**
     * Reset the snackbar message after it has been handled.
     */
    fun resetSnackbarState() {
        _snackbar.value = null
    }

    /**
     * The current filter set applied to the list of stocks.
     */
    private val filters = MutableLiveData<StockFilter>(0)

    /**
     * LiveData object that contains a list of the current stocks from the database.
     *
     * Shown is how the
     */
    val stocks: LiveData<List<Stock>> = filters.switchMap { filter ->
        if (filter == NoFilter) {
            repository.stocks
        } else {
            repository.filterStocksBySubstring("filters")
        }
    }


    init {

        launchDataLoad {
            _snackbar.value = "Test Snackbar Action"
        }

    }

    fun testLoadingBarFunctionality() {
        Log.d("StockViewModel", "testInvocationFromOnClick()")


        launchDataLoad {
            _snackbar.value = "Test Snackbar Action"
            delay(5000)
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _loadingState.value = true
                block()
            } catch (error: Throwable) {
                _snackbar.value = error.message
            } finally {
                // stop loading bar when done
                _loadingState.value = false
            }
        }
    }

}


/**
 * Factory class, which serves up new instances of the to provide the repository singleton to the
 * ViewModels that are created.
 *
 * This way we avoid worrying about multiple instances of the Stocks repository across fragments.
 *
 * e.g. if there were multiple collections of stocks, one for watch list, one for long positions,
 * and a third for short positions; we wouldn't need to manage three different repositories for
 * accessing the same data.
 */
class StockListViewModelFactory(
    private val repository: StockDataRepository
) : ViewModelProvider.NewInstanceFactory() {

    @FlowPreview
    @ExperimentalCoroutinesApi
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = StocksViewModel(repository) as T
}