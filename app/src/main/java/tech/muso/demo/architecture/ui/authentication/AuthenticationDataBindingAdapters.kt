package tech.muso.demo.architecture.ui.authentication

import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.databinding.BindingAdapter
import tech.muso.demo.architecture.R
import tech.muso.demo.architecture.viewmodels.authentication.ErrorState

/**
 * Using data binding, connect the error state contained within this ViewModel to an animation.
 *
 * The animation is applied on an entry error.
 */
@BindingAdapter("app:errorState")
fun bindErrorAnimationToView(view: View, errorState: ErrorState) {
    if (errorState.isError) {
        val animationShake = AnimationUtils.loadAnimation(view.context, R.anim.anim_error_shake).apply {
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationRepeat(animation: Animation?) {

                }

                override fun onAnimationEnd(animation: Animation?) {
                    // perform callback if it was set
                    errorState.callback?.invoke()
                }

                override fun onAnimationStart(animation: Animation?) {

                }
            })
        }
        view.startAnimation(animationShake)
    }
}