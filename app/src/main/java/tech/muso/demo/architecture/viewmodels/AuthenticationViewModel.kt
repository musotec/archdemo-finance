package tech.muso.demo.architecture.viewmodels

import android.app.Application
import androidx.lifecycle.*
import tech.muso.demo.architecture.view.NumberPadEntryView

/**
 * ViewModel that will be used for "authentication" of the pin or any other credentials.
 */
class AuthenticationViewModel(val exampleArgPinLength: Int) : ViewModel() {

    // hidden LiveData so we can observe changes to the pin and perform callbacks when needed
    private var _pin: MutableLiveData<String> = MutableLiveData("")
    val pin: LiveData<String>? get() = _pin

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