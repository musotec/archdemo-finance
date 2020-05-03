package tech.muso.demo.repos

import androidx.lifecycle.LiveData
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.database.api.StockDao
import tech.muso.demo.repos.api.StockRepo

/**
 * TODO: This is a placeholder for development of the UI/ViewModel
 */
class StockDataRepository private constructor(
    private val stockDao: StockDao
): StockRepo {

    // java style singleton object for this class
    companion object {
        @Volatile
        private var instance: StockDataRepository? = null

        fun getInstance(stockDao: StockDao) =
            instance ?: synchronized(this) {
                instance ?: StockDataRepository(stockDao).also { instance = it }
            }
    }

    override val stocks: LiveData<List<Stock>>
        get() = stockDao.getStocks()

    override fun filterStocksBySubstring(search: String): LiveData<List<Stock>> {
        TODO("Not yet implemented")
    }

}