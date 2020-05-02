package tech.muso.demo.database.test.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.database.api.StockDao

/**
 * Class for testing Dao operations within the :repos module, without a room database (in :database)
 *
 * Note: here we don't need to have the Fake implementation private, as we will want to test the UI
 *  against this.
 */
class FakeStockDao : StockDao {

    private val stocksList = mutableListOf<Stock>()
    private val stocks = MutableLiveData<List<Stock>>()

    init {
        stocks.value = stocksList
    }

    fun addStock(stock: Stock) {
        stocksList.add(stock)
        stocks.value = stocksList
    }

    override fun getStocks(): LiveData<List<Stock>> = stocks

    override fun getStocksByName(search: String): LiveData<List<Stock>> {
        TODO("Not yet implemented")
    }

    override suspend fun insertAll(stocks: List<Stock>) {
        this.stocks.value = stocks
    }

}