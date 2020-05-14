package tech.muso.demo.repos.utils

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.database.test.data.FakeStockDao
import tech.muso.demo.repos.StockDataRepository

/**
 * Define an interface that specifies what we need to provide for our manual dependency injection.
 */
interface StocksViewModelFactoryProvider {
    fun provideStocksViewModelFactory(context: Context): ViewModelProvider.NewInstanceFactory
}

abstract class TestStocksViewModelProvider:
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
