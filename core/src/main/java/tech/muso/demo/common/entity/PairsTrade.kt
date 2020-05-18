package tech.muso.demo.common.entity

data class PairsTrade(val shortPosition: Stock, val longPosition: Stock) {
    val name: String get() = "${longPosition.symbol}-${shortPosition.symbol}\nPairs Trade"
    val profit: Double get() = shortPosition.profit + longPosition.profit
    val profitDay: Double get() =
                (shortPosition.currentPrice - shortPosition.openPrice) +
                (longPosition.currentPrice - longPosition.openPrice)
    val notionalValue: Double get() = longPosition.currentPrice - shortPosition.currentPrice
}