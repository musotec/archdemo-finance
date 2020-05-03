package tech.muso.demo.repos

import androidx.annotation.AnyThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A mock repository object that provides access for the [AuthenticationViewModel].
 *
 * Since this would likely be an API, we have made this a Singleton.
 *
 * This is so that we can create and maintain a single connection across all the
 */
object AuthenticationRepository {

    class Connection {
        var test: String = ""
    }

    private val _isAppUnlocked = MutableLiveData<Boolean>(false)
    val isAppUnlocked: LiveData<Boolean>
        get() = _isAppUnlocked

    /**
     * Perform a "test" of the pin.
     *
     * Simply waits 1 or 2 seconds before unlocking or remaining locked.
     */
    @AnyThread
    suspend fun testPinValidity(pin: String?): Boolean {
        if (pin == fetchValidPin()) {
            delay(2000)
            // explicitly update the livedata value on the main thread,
            // so that we can call this from any other safely
            CoroutineScope(Dispatchers.Main).launch {
                // wait a bit before we "unlock"
                // it is important to note that this does not block the main thread
                delay(750)
                _isAppUnlocked.value = true
            }
            return true
        } else {
            // delays in this scope however do effectively block the code
            // from executing after any call to this method
            delay(1000)
        }
        return false
    }

    /**
     * One-shot operation to get the valid pin from the server or cache for validating against.
     */
    suspend fun fetchValidPin(): String {
        // would normally do some sort of network fetch;
        // likely caching this for future use without an active network connection

        // e.g. retrofitClient.doSomething(operation)

        delay(500) // just delay to simulate fetch
        return "1234"
    }

    // a real one-shot job might look like this, with callbacks to perform subsequent network calls
//    suspend fun doOneShot(param: String) : Result<String> =
//        suspendCancellableCoroutine { continuation ->
//            api.addOnCompleteListener  { result ->
//                continuation.resume(result)
//            }.addOnFailureListener { error ->
//                continuation.resumeWithException(error)
//            }
//
//            // NOTE: continuation.resume() is ignored on cancel of coroutine.
//        }
}