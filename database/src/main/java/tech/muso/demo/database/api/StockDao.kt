package tech.muso.demo.database.api

import androidx.lifecycle.LiveData
import tech.muso.demo.common.entity.Stock

/**
 * The DAO (Data Access Object) for the Stock class.
 * This defines what methods we want to expose as endpoints for interfacing with the database.
 */
interface StockDao {
    /** @return LiveData that holds a new list of [Stock] objects any time the table is updated. */
    fun getStocks(): LiveData<List<Stock>>
}
