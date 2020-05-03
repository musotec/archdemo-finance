package tech.muso.demo.architecture.ui.stocks

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import tech.muso.demo.common.entity.Profile

/**
 * Create a custom DataBinding adapter so that we can hide the image loading logic of Glide.
 *
 * The [Profile] object only contains the image url for the stock, but could potentially handle
 * bindings for a more complex object (logo + background, graph, etc.)
 */
@BindingAdapter("stockProfile")
fun bindImageFromUrl(view: ImageView, stockProfile: Profile) {
    val imageUrl: String? = stockProfile.logoUrl
    if (!imageUrl.isNullOrEmpty()) {
        Glide.with(view.context)
            .load(imageUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(view)
    }
}