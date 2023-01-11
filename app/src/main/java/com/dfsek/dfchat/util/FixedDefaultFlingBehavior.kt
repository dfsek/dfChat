package com.dfsek.dfchat.util

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

/**
 * Fixes Jetpack Compose bug which causes fling-scrolling to break if system animations are disabled.
 */
class FixedDefaultFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>,
    private val scope: CoroutineScope
) : FlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return if (abs(initialVelocity) > 1f) {
            val context = coroutineContext
            scope.async(context = MotionDurationScaleImpl) { // this is literally only so we can put the correct value for animation scale into the context.
                var velocityLeft = initialVelocity
                var lastValue = 0f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                ).animateDecay(flingDecay) {
                    if(!context.isActive) this.cancelAnimation()
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed
                    if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                }
                velocityLeft
            }.await()

        } else {
            initialVelocity
        }
    }
    companion object {
        @Composable
        fun fixedFlingBehavior(): FlingBehavior {
            val flingSpec = rememberSplineBasedDecay<Float>()
            val scope = rememberCoroutineScope()
            return remember(flingSpec) {
                FixedDefaultFlingBehavior(flingSpec, scope)
            }
        }
    }
}

private object MotionDurationScaleImpl : MotionDurationScale {
    override val scaleFactor = 1f
}
