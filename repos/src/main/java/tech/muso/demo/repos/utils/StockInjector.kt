package tech.muso.demo.repos.utils

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.database.api.StockDb
import tech.muso.demo.database.test.data.FakeStockDao
import tech.muso.demo.repos.StockDataRepository
import tech.muso.demo.repos.service.StockWebService

/**
 * Define an interface that specifies what we need to provide for our manual dependency injection.
 */
interface StocksViewModelFactoryProvider {
    fun provideStocksViewModelFactory(context: Context): ViewModelProvider.NewInstanceFactory
}

/**
 * The stocks ViewModel provider. This internally handles the default setup, with the provided
 * Android context.
 *
 * NOTE: when testing. The [setInjectorForTesting] should be used, and not this object!
 *   The Android [Context] should never be called from a test. Even though it is possible to mock
 *   using certain tools.
 *
 *   Writing library code testable without Mockito or Robolectric is more robust!
 *   Writing the tests is also easier! This is essential for small teams!
 */
abstract class LiveStocksViewModelProvider:
    StocksViewModelFactoryProvider {

    @FlowPreview
    @ExperimentalCoroutinesApi
    fun getStockRepository(context: Context): StockDataRepository {
        return StockDataRepository.getInstance(
            stockDao(context),
            stockService()
        )
    }

    private fun stockService() = StockWebService()

    private fun stockDao(context: Context) =
        StockDb.getInstance(context.applicationContext).stockDao
}

/**
 * A ViewModelProvider that uses a FakeStockDao to back it.
 * Any implementation of a StockDataRepository can be used
 */
@VisibleForTesting
private abstract class TestStocksViewModelProvider:
    StocksViewModelFactoryProvider {
        private val fakeStockDao = FakeStockDao()

        fun getStockRepository(): StockDataRepository {
            return StockDataRepository.getInstance(fakeStockDao)
        }

        fun addStock(stock: Stock) {
            fakeStockDao.addStock(stock)
        }
    }

/**
 * Below we create definitions that will be critical for writing unit tests to effectively test the
 * functionality of the ViewModel to Repository connection without requiring an actual
 * Room or SQL database.
 *
 * Since that would require a context, we can write robust tests without Mockito/Robolectric.
 */

@VisibleForTesting
private val StockInjector: StocksViewModelFactoryProvider?
    get() = currentInjector

private object Lock

@Volatile private var currentInjector: StocksViewModelFactoryProvider? = null

/**
 * Manually specify the injector to use during testing (within this module).
 * This allows for test implementations to avoid using the default provider, which may rely on
 * android contexts (os restriction), hardware components, or network in a way that is undesirable.
 *
 * @param injector the ViewModelProvider used for testing purposes. If null, the default is used.
 */
@VisibleForTesting
private fun setInjectorForTesting(injector: StocksViewModelFactoryProvider?) {
    synchronized(Lock) {
        currentInjector = injector
    }
}

@VisibleForTesting
private fun resetInjector() =
    setInjectorForTesting(null)
