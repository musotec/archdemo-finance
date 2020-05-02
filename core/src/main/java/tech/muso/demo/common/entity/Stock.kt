package tech.muso.demo.common.entity

import androidx.room.*
import tech.muso.demo.common.valueobject.StockValueObject

/**
 * The entity objects represent specific objects, and are not defined by their attributes.
 *
 * This way you can have multiple unique instances that are mutable.
 *
 * For this StockEntry example, the price of the stock can change, so the class is an entity.
 * This allows for separation from the Value Object variant, which should only contain attributes
 * common across instances, and that do not change after creation. Like ticker symbol, etc.
 */
@Entity()
data class Stock(
    @PrimaryKey @ColumnInfo(name = "id") val symbol: String,
    val name: String,
    @Embedded val fundamentals: Fundamentals,
    @Embedded val profile: Profile
) {

    var currentPrice: Double = 0.0
    var lastTradeVolume: Double = 0.0 // Double due to partial shares.

    // convenient reference of lightweight value object for comparison of non-changing data
    @Ignore // tell Room not to store this value object
    val valueObject: StockValueObject = StockValueObject(symbol)

    /**
     * Stocks should be equal if the two value objects are the same.
     * (the stock symbol will always represent the same company name, date of foundation, etc.)
     * This is faster than doing a full object comparison.
     *
     * The Stock will always have the same current price, last trade volume, etc.
     *
     * In an example use case, we are doing a comparison between two stock lists.
     * One Stock is updated with price information; while the other Stock object is in a watchlist.
     * Because the two objects contain different information as entities, we still want them to pass
     * an equality check.
     *
     * If we had two accounts with different amounts of the same stock, that case should use a
     * different Entity class. Then, the two holdings would always have different values unless they
     * are truly the same object.
     */
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false
        other as Stock
        return valueObject == other.valueObject
    }
}