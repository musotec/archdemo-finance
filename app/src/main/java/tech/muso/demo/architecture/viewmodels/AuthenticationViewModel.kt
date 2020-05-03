package tech.muso.demo.architecture.viewmodels

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.launch
import tech.muso.demo.architecture.BuildConfig
import tech.muso.demo.architecture.helper.safeLambdaApply
import tech.muso.demo.architecture.view.HiddenPinView
import tech.muso.demo.architecture.view.NumberPadEntryView
import tech.muso.demo.architecture.viewmodels.jobs.PinLoginJob
import tech.muso.demo.repos.AuthenticationRepository
import java.lang.ref.WeakReference

// define type aliases to improve code readability
data class ErrorState(val isError: Boolean, val callback: (() -> Unit) ?= null)

// This pin observe interface is the WRONG way to do data binding and MVVM architecture.
// Since the business logic is now directly attached to the View. We have overstepped our separation
// of concerns. The only safe way to do this without leaking, is to use a WeakReference and then
// trust that we don't leak the view later. The only reason we would need to do something like this
// is if it was extremely critical that the MutableLiveData object not be exposed to accessors of
// the ViewModel. This achieves that, but it complicates code, is not necessary here, and is not
// implemented or commented in a way that would warn other developers to avoid the practice.

// So the code surrounding this logic will be refactored immediately to show how a code review can
// catch these things early so that code does not get committed that can introduce difficult to
// diagnose bugs that can remain dormant for a long time. Especially when it is not needed.
typealias PinObserveInterface = (WeakReference<HiddenPinView>) -> Unit

/**
 * ViewModel that will be used for "authentication" of the pin or any other credentials.
 */
class AuthenticationViewModel(val exampleArgPinLength: Int) : ViewModel() {

    // hidden LiveData so we can observe changes to the pin and perform callbacks when needed
    private var _pin: MutableLiveData<String> = MutableLiveData("")
    // expose (only in debug builds) the pin for demonstration visibility
    val _exposedForDemoPin: LiveData<String>? get() = if (BuildConfig.DEBUG) _pin else null

    /**
     * Whether or not the PIN entry has had an error.
     * This is used to connect Views to [attachErrorAnimationToView] to perform the error animation.
     */
    val incorrectEntryErrorState: LiveData<ErrorState> get() = _incorrectEntryErrorState
    private val _incorrectEntryErrorState = MutableLiveData(ErrorState(false))

    /**
     * Declare internal callback lambdas to connect display functionality to the [HiddenPinView]
     * without exposing our sensitive objects.
     */
    private var callbackShowHint: (() -> Unit)? = null
    private var callbackHideHint: (() -> Unit)? = null
    private var callbackUpdatePinView: (() -> Unit)? = null

    init {
        // this binding allows for us to execute our demonstration pin validation job.
        // in a real application, this job would be something run on the server in the background.
        _pin.observeForever { pin ->
            callbackUpdatePinView?.invoke()

            if (pin.length == exampleArgPinLength)
                validatePinJob.start()

            callbackShowHint?.invoke()
        }
    }

    /**
     * Demonstration "network" job. Where we launch a test via our server connection, which would
     * validate the input against whatever server authentication.
     */
    private val validatePinJob: PinLoginJob = object : PinLoginJob() {
        override suspend fun doBackgroundWork() {
            // first hide our hint to add a little suspense
            callbackHideHint?.invoke()

            if(!AuthenticationRepository.testPinValidity(_pin.value)) {
                // from this check we can launch a callback on the viewModelScope
                // to update the ViewModel on the correct thread (the UI thread)
                viewModelScope.launch {
                    // this is just to demonstrate concepts, but should be standardized
                    if (_pin.value?.length == exampleArgPinLength) {
                        _incorrectEntryErrorState.value = ErrorState(true) {
                            // when animation is finished, reset the pin value
                            _pin.value = ""
                        }
                    }
                }
            }
        }

        override suspend fun onWorkFinished() {
            // when the pin has been tested, perform our demo "pin hint" indicator
            callbackShowHint?.invoke()
        }

        override suspend fun onJobTimeout() {
            _pin.value = "tout"
        }
    }

    /**
     * Helper extension function for the backspace() function.
     * This does the length checking and index coercion to avoid IndexOutOfBoundsExceptions.
     */
    @Suppress("NOTHING_TO_INLINE")  // suppress warning; this saves a method call
    private inline fun MutableLiveData<String>.backspace() {
        this.value = this.value?.run { substring(0, (length - 1).coerceAtLeast(0)) }
    }

    /**
     * If we had to set a new listener in this ViewModel (for whatever reason),
     * to have the connected [NumberPadEntryView] automatically handle the new one correctly
     * we would need to recall Binding#executePendingBindings() for the [NumberPadEntryView].
     */
    val numberPadListener = object : NumberPadEntryView.NumberPadOnClickListener {

        override fun onNumKeyPress(number: Int) {
            // if already at the maximum pin length, wait for the validation to finish first
            // and ignore the users inputs (purposefully not queued while device "thinking")
            if (_pin.value?.length ?: 0 >= exampleArgPinLength) {
                return
            }

            // append to the value
            _pin.value += number
        }

        override fun onBackKeyPress() {
            _pin.backspace()
        }
    }

    /**
     * Create a hidden observer that attaches to the PinEntryView,
     * and updates it with the currently entered pin validity.
     *
     * This structure is used so that we do not expose our business logic and our backing pin value.
     * Instead of keeping the
     */
    val pinObserveInterface: PinObserveInterface = { hiddenPinView ->
        // when our pin view is attached,

        // create a callback to pass the backing pin data (properly) to the hidden pin view
        callbackUpdatePinView = hiddenPinView.safeLambdaApply {
            text = _pin.value
        }

        callbackShowHint = hiddenPinView.safeLambdaApply {
            // launch on main thread to update UI and allow for suspend function invocation
            viewModelScope.launch {

                // fetch valid pin
                val validPin = AuthenticationRepository.fetchValidPin()

                // test matching values and store index of good and bad matches
                _pin.value?.let {
                    val goodList = mutableListOf<Int>()
                    val badList = mutableListOf<Int>()

                    it.forEachIndexed { index, c ->
                        // exit for excessive index.
                        if (index >= validPin.length) return@forEachIndexed
                        if (c == validPin[index]) goodList.add(index) else badList.add(index)
                    }

                    // send them to the PinEntryView for display
                    setCorrectIndexList(goodList)
                    setIncorrectIndexList(badList)
                }
            }
        }

        // and another that will remove it.
        callbackHideHint = hiddenPinView.safeLambdaApply {
            viewModelScope.launch {
                // must update the PinEntryView on the UI thread
                // viewModelScope is used so that we only perform this when the ViewModel is active
                clearPinValidityLists()
            }
        }
    }

    // in onCleared() we would want to remove any other LiveData/observable subscriptions that we
    // have in this class, to avoid leaking this ViewModel. However, there is no need to do so here.
    override fun onCleared() {
        super.onCleared()
    }

    /**
     * Create a factory that would generate a class with some parameter, whether it be an arbitrary
     * pin length, or a secret key. The class would then exist as a singleton object.
     * We want a single authentication ViewModel because real components would likely be Web APIs.
     *
     * Additionally, this ViewModel could be created and used for both login, pin entry, and
     * account creation.
     *
     * This provides us a simple way to handle the business logic for any of these actions.
     * The Views/Fragments can then share the single
     *
     * @param exampleArgPinLength exists as an arbitrary example of why this factory would be
     *   necessary, due to the fact that the ViewModel's primary constructor may require a parameter
     *   for any reason. But would not be invokable via [androidx.lifecycle.ViewModelProviders.of].
     */
    class Factory(private val exampleArgPinLength: Int, application: Application)
        : ViewModelProvider.AndroidViewModelFactory(application) {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(Int::class.java).newInstance(exampleArgPinLength)
        }
    }
}