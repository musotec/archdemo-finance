package tech.muso.demo.architecture.viewmodels.stocks

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.databinding.*
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import tech.muso.demo.architecture.R
import tech.muso.demo.common.entity.Fundamentals
import tech.muso.demo.common.entity.Profile
import tech.muso.demo.common.entity.Stock
import tech.muso.demo.common.entity.Stock.Companion.deepCopy
import tech.muso.demo.repos.StockDataRepository
import java.util.*
import kotlin.math.absoluteValue


@BindingAdapter("app:ratio_link")
fun bindRatioLink(view: Slider, viewModel: StocksTradePairsViewModel) {
    view.addOnChangeListener { slider, value, fromUser ->
        viewModel.ratio.value = value
    }
}

@BindingAdapter("app:short_long_state")
fun bindButtonShortLongState(view: MaterialButton, isShort: Boolean) {
    if (isShort) {
        view.rippleColor = ContextCompat.getColorStateList(view.context, R.color.colorAccentRed)
        view.setTextColor(ContextCompat.getColorStateList(view.context, R.color.colorAccentRed))
    } else {
        view.rippleColor = ContextCompat.getColorStateList(view.context, R.color.colorSecondary)
        view.setTextColor(ContextCompat.getColorStateList(view.context, R.color.colorSecondary))
    }
}

@SuppressLint("ClickableViewAccessibility")
@BindingAdapter("app:bindCursorController")
fun bindCursorController(view: EditText, viewModel: StocksTradePairsViewModel) {
    var lastCursorPosition = 0
    var fromUser = false
    view.setOnTouchListener { v, event ->
        when(event.action) {
            MotionEvent.ACTION_DOWN -> fromUser = true
        }
        return@setOnTouchListener false
    }
    // if the user starts typing, then they want to lock.
    view.setOnKeyListener { v, keyCode, event ->
        viewModel.priceLocked.value = true
        return@setOnKeyListener false
    }

    view.accessibilityDelegate = object : View.AccessibilityDelegate() {
        override fun sendAccessibilityEvent(host: View?, eventType: Int) {
            super.sendAccessibilityEvent(host, eventType)
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                // when from the user, (handled via touch, always accept new position)
                if (fromUser) {
                    lastCursorPosition = view.selectionStart
                    fromUser = false

                    // if we are selecting a range, lock value.
                    if (view.selectionStart != view.selectionEnd) {
                        viewModel.priceLocked.value = true
                    }

                    return
                }

                // if we aren't locked, keep cursor in same position (if in range)
                else if (viewModel.priceLocked.value == false
                    && lastCursorPosition != view.selectionStart
                    && lastCursorPosition <= view.text.length) {
                    view.setSelection(lastCursorPosition)
                    view.isCursorVisible = true
                }

                else if (lastCursorPosition != view.selectionStart) {
                    lastCursorPosition = view.selectionStart
                }
            }
        }
    }
}

@BindingAdapter("app:observe")
fun bindLockIcon(view: MaterialButton, isLocked: Boolean) {
    val lockIconRes = if (isLocked) R.drawable.ic_baseline_lock_24 else R.drawable.ic_baseline_lock_open_24
    view.icon = view.resources.getDrawable(lockIconRes,null)
}

@ExperimentalCoroutinesApi
@FlowPreview
/**
 * A ViewModel that controls the business logic for handling a Pairs Trade.
 * This connects the View to the same StockRepository as the List, but contains logic specifically
 * for performing the Pairs Trade feature.
 */
class StocksTradePairsViewModel internal constructor(
    private val repository: StockDataRepository
) : ViewModel() {

    /**
     * LiveData object that contains a list of the current stocks from the database.
     */
    val stocks: LiveData<List<Stock>> = repository.stocks

    private val _first = MutableLiveData<Stock>().also {
//        it.asFlow().map {
//            // could monitor for changes to our stocks here and update the price this way.
//        }.flowOn(Dispatchers.IO)
    }
    val first: LiveData<Stock>
        get() = _first

    private val _second = MutableLiveData<Stock>()
    val second: LiveData<Stock>
        get() = _second

    val ratio = MutableLiveData<Float>(0.5f)//.also {
//        it.asFlow().map { floatRatio ->
//            // compute our nearest fraction
//            with(ratioToFractionTuple(floatRatio)) {
//                viewModelScope.launch {
//                    _first.value = _first.value?.deepCopy()?.apply { amount = first }
//                    _second.value = _second.value?.deepCopy()?.apply { amount = second }
//                }
//            }
//        }.flowOn(Dispatchers.IO)
//    }

        .apply {
            observeForever( Observer { floatRatio ->
            // compute our nearest fraction
                CoroutineScope(Dispatchers.IO).launch {
                    with(ratioToFractionTuple(floatRatio)) {
                        viewModelScope.launch {
                            _first.value = _first.value?.deepCopy()?.apply {
                                val wasShort = isShort
                                amount = first
                                if (isShort != wasShort) amount *= -1
                            }
                            _second.value = _second.value?.deepCopy()?.apply {
                                val wasShort = isShort
                                amount = (first - second) // adjust "second" is denominator, so total adds to 1
                                if (isShort != wasShort) amount *= -1
                            }
                            // also unlock because price will be wrong
                            priceLocked.value = false
                        }
                    }
                }
            } )
        }

    /**
     * Compute the nearest fraction between [0, 1) by walking a Stern-Brocot tree.
     *
     * Runtime: O(log2(n))
     */
    private suspend fun ratioToFractionTuple(ratio: Float, errorMargin: Float = 0.05f): Pair<Int, Int> {
        // lower bound of tree 0/1
        var lowerNumerator = 0
        var lowerDenominator = 1
        // upper bound of tree 1/1
        var upperNumerator = 1
        var upperDenominator = 1

        while(true) {
            // calculate the mediant between upper and lower
            val midNumerator = lowerNumerator + upperNumerator
            val midDenominator = lowerDenominator + upperDenominator
            when {
                // if below the middle, and outside margin of error; walk down tree to the left
                midDenominator * (ratio + errorMargin) < midNumerator -> {
                    // walk left, mediant becomes upper bound
                    upperNumerator = midNumerator
                    upperDenominator = midDenominator
                }
                midNumerator < (ratio - errorMargin) * midDenominator -> {
                    // walk right, mediant becomes lower bound
                    lowerNumerator = midNumerator
                    lowerDenominator = midDenominator
                }
                else -> {
                    // exit condition, where we are within the margin of error
                    return Pair(midNumerator, midDenominator)
                }
            }
        }
    }

    var chartVisibility = MutableLiveData<Int>(View.VISIBLE)
    var priceLocked = MutableLiveData<Boolean>(false)

    init {
        val firstStock =
            Stock(
                "SPY",
                "Spooders",
                Fundamentals(0, 0.0, 0.0, 0),
                Profile("")
            ).apply {
                this.currentPrice = 282.9
                this.priceChangePercent = 0.0
                this.amount = 1
            }

        val secondStock =
            Stock(
                "QQQ",
                "Queues",
                Fundamentals(0, 0.0, 0.0, 0),
                Profile("")
            ).apply {
                this.currentPrice = 225.22
                this.priceChangePercent = 0.0
                this.amount = -1
            }

        _first.value = firstStock
        _second.value = secondStock

        launchDataLoad {
            repository.tryUpdateStockCache()
        }
    }

    fun toggleShortLongSecondStock() {
        _second.value = second.value?.deepCopy()?.also {
            it.amount = -it.amount
        }
    }

    fun toggleShortLongFirstStock() {
        _first.value = first.value?.deepCopy()?.also {
            it.amount = -it.amount
        }
    }

    fun swapAmounts(view: View) {
        var _secondAmountCached = -1
        _second.value = second.value?.deepCopy()?.also {
            _secondAmountCached = it.amount
            it.amount = first.value?.amount ?: 1
        }
        _first.value = first.value?.deepCopy()?.also {
            it.amount = _secondAmountCached
        }
    }

    fun sendOrder(view: View) {
        viewModelScope.launch {
            Snackbar.make(view, "${first.value?.amount?.absoluteValue}:${second.value?.amount?.absoluteValue} Pairs trade sent! ${first.value?.symbol}-${second.value?.symbol} @ ${"%.2f".format(orderPrice)}", Snackbar.LENGTH_SHORT).show()
        }
        priceLocked.value = false
    }

    fun toggleLock(view: View) {
        if (view !is MaterialButton) return // only do this if material button, not the best
        val newPriceLockedValue = !(priceLocked.value?:false)
        priceLocked.value = newPriceLockedValue
    }

    fun testLoadingBarFunctionality() {
        Log.d("StockViewModel", "testInvocationFromOnClick()")

        viewModelScope.launch {
            delay(5000)
        }
    }

    suspend fun totalPrice(): Double {
        val totalPriceFirst = first.value?.totalPrice ?: 0.0
        val totalPriceSecond = second.value?.totalPrice ?: 0.0
        return totalPriceFirst + totalPriceSecond
    }

    fun onOrderPriceChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        orderPrice = s.toString().toDoubleOrNull() ?: 0.0
    }

    private var orderPrice: Double = 0.0

    val currentPrice: LiveData<Double> = flow {

        var firstPrice = first.value?.currentPrice ?: 0.0
        var secondPrice = second.value?.currentPrice ?: 0.0

        val freq = 100

        val random = Random()
        fun generateNextTick() {
            val delta = random.nextGaussian() / freq
            firstPrice += delta
            val delta2 = random.nextGaussian() / freq
            secondPrice += delta2
        }

        // this looks dangerous but is completely fine because of how coroutines work.
        // we don't need an exit condition because the Dispatchers.IO scope will handle it.
        while (true) {
            generateNextTick()
            delay(freq * 10L)
            viewModelScope.launch {
                _first.value = first.value?.deepCopy()?.apply { currentPrice = firstPrice }
                _second.value = second.value?.deepCopy()?.apply { currentPrice = secondPrice }
            }
            // if we are price locked, don't compute the price
            if (priceLocked.value == true) continue
            // emit price computation
            emit(totalPrice())
        }

    }.flowOn(Dispatchers.IO).asLiveData()

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
//            try {
//                _loadingState.value = true
//                block()
//            } catch (error: Throwable) {
//                _snackbar.value = error.message
//            } finally {
//                // stop loading bar when done
//                _loadingState.value = false
//            }
        }
    }

}