package tech.muso.demo.architecture.viewmodels.authentication

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tech.muso.demo.architecture.BuildConfig
import tech.muso.demo.architecture.view.HiddenPinView
import tech.muso.demo.architecture.view.NumberPadEntryView
import tech.muso.demo.architecture.viewmodels.authentication.jobs.PinLoginJob
import tech.muso.demo.repos.AuthenticationRepository

// define type aliases to improve code readability
data class ErrorState(val isError: Boolean, val callback: (() -> Unit) ?= null)

/**
 * ViewModel that will be used for "authentication" of the pin or any other credentials.
 */
class AuthenticationViewModel(val exampleArgTotalPinLength: Int) : ViewModel() {

    // hidden LiveData so we can observe changes to the pin and perform callbacks when needed
    private var _pin: MutableLiveData<String> = MutableLiveData("")
    // expose (only in debug builds) the pin for demonstration visibility
    val _exposedForDemoPin: LiveData<String>? get() = if (BuildConfig.DEBUG) _pin else null

    /**
     * Whether or not the PIN entry has had an error.
     * This is used to connect Views to [attachErrorAnimationToView] to perform the error animation.
     */
    val incorrectEntryErrorState: LiveData<ErrorState> get() = _incorrectEntryErrorState
    private val _incorrectEntryErrorState = MutableLiveData(
        ErrorState(
            false
        )
    )

    private val _showPinHint: MutableLiveData<Boolean> = MutableLiveData(false)

    /**
     * Using Kotlin Coroutines Flow, map the previous LiveData and emit a state for our view
     * that has all the information about the pin validity. This is super easy to chain network
     * calls with, and we don't have to worry about adding observers, since the resulting LiveData
     * will handle that when it is bound to the view.
     */
    val pinValidityState: LiveData<HiddenPinView.PinValidityState> =
        _showPinHint.asFlow().map { isShowHint ->
            if (isShowHint) {
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
                    // return validity state
                    return@map HiddenPinView.PinValidityState(true, goodList, badList)
                }
            }
            // else return default state (no highlight)
            return@map HiddenPinView.PinValidityState()
        }.asLiveData()

    val pinViewState: LiveData<HiddenPinView.PinViewState> =
        _pin.asFlow().map { pin ->
            // any time the pin updates, hide our previous pin hint
            _showPinHint.value = false

            // if the pin is full length, run the validation job
            // in a real application, this job would be something run on the server in the background.
            if (pin.length == exampleArgTotalPinLength) {
                validatePinJob.start()
            }

            // always start this job, we save it (so we can cancel it via the validatePinJob)
            pendingPinHintJob = viewModelScope.launch {
                delay(100)          // delay is artificial, but shows cancellation of Jobs
                _showPinHint.value = true    // if we set this value to false before we execute
            }

            return@map HiddenPinView.PinViewState(pin.length, exampleArgTotalPinLength)
        }.asLiveData()

    var pendingPinHintJob: Job? = null

    /**
     * Demonstration "network" job. Where we launch a test via our server connection, which would
     * validate the input against whatever server authentication.
     */
    private val validatePinJob: PinLoginJob = object : PinLoginJob() {
        override suspend fun doBackgroundWork() {
            // first hide our hint to add more suspense than 100ms hint wait previously
            // instead we will wait until onWorkFinished()
            _showPinHint.postValue(false) // postValue forces LiveData on next ui call

            if(!AuthenticationRepository.testPinValidity(_pin.value)) {
                // from this check we can launch a callback on the viewModelScope
                // to update the ViewModel on the correct thread (the UI thread)
                viewModelScope.launch {
                    // this is just to demonstrate concepts, but should be standardized
                    if (_pin.value?.length == exampleArgTotalPinLength) {
                        _incorrectEntryErrorState.value =
                            ErrorState(
                                true
                            ) {
                                // when animation is finished, reset the pin value
                                _pin.value = ""
                            }
                    }
                }
            }
        }

        override suspend fun onWorkFinished() {
            // when the pin has been tested, perform our demo "pin hint" indicator)
            _showPinHint.value = true
        }

        override suspend fun onJobTimeout() {
            // todo: demonstrate server timeout & error state handling.
            _pin.value = "tout"
        }
    }

    /**
     * Reset the entered pin.
     */
    fun clearPin() {
        _pin.postValue("")
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
            if (_pin.value?.length ?: 0 >= exampleArgTotalPinLength) {
                return
            }

            // append to the value
            _pin.value += number
        }

        override fun onBackKeyPress() {
            _pin.backspace()
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