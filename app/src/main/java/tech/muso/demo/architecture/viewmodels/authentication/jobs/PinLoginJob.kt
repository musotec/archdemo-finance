package tech.muso.demo.architecture.viewmodels.authentication.jobs

import android.util.Log
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import java.util.concurrent.CancellationException

/**
 * A Coroutine Job that can be reused by our code to perform network checks in the background.
 *
 * All the logic here just emulates some network delay in the background.
 */
open class PinLoginJob {

    private val JOB_TIMEOUT = 5000L // ms timeout
    private lateinit var job: CompletableJob

    init {
        initJob()
    }

    /**
     * Create our Job, with standardized onCompletionListener to handle errors.
     */
    private fun initJob() {
        job = Job()
        job.invokeOnCompletion {
            when(it) {
                is TimeoutCancellationException -> {
                    CoroutineScope(Dispatchers.Main).launch {
                        onJobTimeout()
                    }
                }
                is CancellationException -> {
                    // do nothing if we cancel, only if we timeout or error.
                    Log.d("PinJob", it.message ?: "CancellationException - no message")
                }
            }
        }
    }

    /**
     * Reset the job, cancelling it if in progress.
     */
    fun reset() {
        if (job.isActive || job.isCompleted) {
            job.cancel(CancellationException("Resetting Pin Login Job"))
        }
        initJob()
    }

    /**
     * Start the job, resetting it if already in progress.
     */
    fun start() {
        // restart the job if already active
        if (this.job.isActive) {
            reset()
        }

        // execute job on the main scope, the job is added to the scope
        CoroutineScope(Dispatchers.IO + job).launch {
            // specify our timeout for the work
            withTimeout(JOB_TIMEOUT) {
                // do the work for the job
                doBackgroundWork()
                // after the work is done, launch our callback on the main thread
                CoroutineScope(Dispatchers.Main).launch {
                    onWorkFinished()
                }
            }
        }
    }

    /**
     * Execute the work on a background thread for this Job.
     */
    @WorkerThread
    open suspend fun doBackgroundWork() {
        delay(500)
    }

    /**
     * Execute a callback on the Main/UI thread when all work is finished.
     */
    @UiThread
    open suspend fun onWorkFinished() {

    }

    /**
     * Execute a callback on the Main/UI thread upon failure.
     */
    @UiThread
    open suspend fun onJobTimeout() {

    }
}