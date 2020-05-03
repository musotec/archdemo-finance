@file:Suppress("NOTHING_TO_INLINE")

package tech.muso.demo.architecture.helper

import java.lang.ref.WeakReference

/**
 * Custom helper function to create a lambda without the verbiage around obtaining the object
 * in the WeakReference.
 *
 * @sample
 * Usage:
 *   weakReferenceObject.safeLambdaApply {
 *       // code here acts as if written `object.apply{ ... }`
 *   }
 *
 * @return returns a lambda that will safely execute as if the held object is non-null
 */
inline fun <T> WeakReference<T>.safeLambdaApply(noinline callback: T.() -> Unit): (() -> Unit)? {
    return {
        get()?.apply {
            callback.invoke(this)
        }
    }
}
