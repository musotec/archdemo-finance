package tech.muso.demo.architecture

import android.app.Application
import android.content.Context
import tech.muso.demo.architecture.viewmodels.StockListViewModelFactory
import tech.muso.demo.common.entity.Fundamentals
import tech.muso.demo.common.entity.Profile
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.repos.utils.TestStocksViewModelProvider
import java.util.*
import kotlin.collections.HashMap

/**
 * This class naturally provides us a global singleton for our application.
 *
 * A lot of Dependency Injection libraries use this singleton in order to do the Injection.
 * This is not always necessary, but can usually become the most logical way to achieve it, when the
 * injected parameter is an Android Context.
 */
class DemoArchitectureApplication: Application() {

    // java style singleton object for this class
    companion object {
        @Volatile
        private var instance: DemoArchitectureApplication? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: DemoArchitectureApplication().also { instance = it }
            }
    }


    /**
     * Manual Dependency Injection implementation.
     *
     * Here we are making a map of the pairs of ViewModelFactories and keys.
     * This allows us to have multiple factories at the same time.
     *
     * For instance, if we have each account or portfolio holding a certain list of stocks, we will
     * want each ViewModelFactory to be able to regenerate the ViewModel and provide the Repository
     * and any necessary parameters.
     */
    private val currentStockViewModelProviders: MutableMap<Int, StockListViewModelFactory> = HashMap()

    object StockInjector : TestStocksViewModelProvider() {
        // preemptively passing a context because we will need it for the eventual Room database
        override fun provideStocksViewModelFactory(context: Context): StockListViewModelFactory {
            return StockListViewModelFactory(getStockRepository()).apply {
                val random = Random()

                val stock =
                    Stock(
                        "AMD",
                        "Advance Micro Devices",
                        Fundamentals(0, 0.0, 0.0, 0),
                        Profile("https://logos.m1finance.com/AMD?size=128")
                    ).apply {
                        this.currentPrice = 54.20
                        this.priceChangePercent = 0.0
                    }

                fun generateNextTick() {
                    val delta = random.nextGaussian()
                    stock.currentPrice += delta
                }

                for(i in 0..50) {
                    addStock(
                        // need to do a deep copy because we're being lazy about creating objects
                        stock.deepCopy().also {
                            generateNextTick()
                        }
                    )
                }
            }
        }
    }

    /**
     * Manual dependency injection allows for retrieval and generation of the multiple instances
     * of the StockListViewModelFactory.
     *
     * This returns a specific instance based on a hash mapping of the class of the [selector].
     * All instances of the Factory provide unique singletons.
     */
    fun getStockViewModelProvider(context: Context, selector: Any): StockListViewModelFactory {
        val key = selector.javaClass.hashCode()
        // return the provider if we already have one defined.
        if (currentStockViewModelProviders.containsKey(key)) {
            return currentStockViewModelProviders[key] ?: error("This cannot happen")
        }
        when(selector) {
            else -> {
                return StockInjector.provideStocksViewModelFactory(context).also { factory ->
                    currentStockViewModelProviders[key] = factory
                }
            }
        }
    }
}