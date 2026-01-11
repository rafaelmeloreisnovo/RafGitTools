package com.rafgittools.core.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Haptic Feedback Manager implementing Feature #232 from the roadmap.
 * 
 * Provides consistent haptic feedback throughout the app for:
 * - Button clicks
 * - Long press actions
 * - Success/Error feedback
 * - Pull-to-refresh
 * - Navigation gestures
 * 
 * Standards compliance:
 * - Android Haptics API
 * - Material Design guidelines for haptic feedback
 */
@Singleton
class HapticFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    private var hapticEnabled: Boolean = true
    
    /**
     * Enable or disable haptic feedback globally.
     */
    fun setEnabled(enabled: Boolean) {
        hapticEnabled = enabled
    }
    
    /**
     * Check if haptic feedback is enabled.
     */
    fun isEnabled(): Boolean = hapticEnabled
    
    /**
     * Check if the device supports haptic feedback.
     */
    fun isSupported(): Boolean {
        return vibrator.hasVibrator()
    }
    
    /**
     * Perform a light click haptic feedback.
     * Use for button presses and small interactions.
     */
    fun performClick(view: View? = null) {
        if (!hapticEnabled) return
        
        view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            ?: vibrateLight()
    }
    
    /**
     * Perform a confirm haptic feedback.
     * Use for confirming actions like successful submissions.
     */
    fun performConfirm(view: View? = null) {
        if (!hapticEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                ?: vibrateConfirm()
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                ?: vibrateLight()
        }
    }
    
    /**
     * Perform a reject haptic feedback.
     * Use for error states or rejected actions.
     */
    fun performReject(view: View? = null) {
        if (!hapticEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
                ?: vibrateReject()
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                ?: vibrateMedium()
        }
    }
    
    /**
     * Perform a long press haptic feedback.
     * Use when entering context menus or selection mode.
     */
    fun performLongPress(view: View? = null) {
        if (!hapticEnabled) return
        
        view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            ?: vibrateMedium()
    }
    
    /**
     * Perform a gesture start haptic feedback.
     * Use when initiating swipe gestures.
     */
    fun performGestureStart(view: View? = null) {
        if (!hapticEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
                ?: vibrateLight()
        } else {
            vibrateLight()
        }
    }
    
    /**
     * Perform a gesture end haptic feedback.
     * Use when completing swipe gestures.
     */
    fun performGestureEnd(view: View? = null) {
        if (!hapticEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                ?: vibrateLight()
        } else {
            vibrateLight()
        }
    }
    
    /**
     * Perform a tick haptic feedback.
     * Use for scrolling through discrete items like pickers.
     */
    fun performTick(view: View? = null) {
        if (!hapticEnabled) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
                ?: vibrateTick()
        } else {
            vibrateLight()
        }
    }
    
    /**
     * Perform a success haptic feedback pattern.
     * Use for successful operations like commits, pushes, etc.
     */
    fun performSuccess() {
        if (!hapticEnabled || !isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }
    
    /**
     * Perform an error haptic feedback pattern.
     * Use for failed operations or errors.
     */
    fun performError() {
        if (!hapticEnabled || !isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Double vibration pattern for errors
            val timings = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 200)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }
    
    /**
     * Perform a pull-to-refresh haptic feedback.
     * Use when pull-to-refresh threshold is reached.
     */
    fun performPullToRefresh() {
        if (!hapticEnabled || !isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else {
            vibrateLight()
        }
    }
    
    // Private vibration helper methods
    
    private fun vibrateLight() {
        if (!isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }
    
    private fun vibrateMedium() {
        if (!isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }
    
    private fun vibrateConfirm() {
        if (!isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            vibrator.vibrate(effect)
        } else {
            vibrateMedium()
        }
    }
    
    private fun vibrateReject() {
        if (!isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            vibrator.vibrate(effect)
        } else {
            vibrateMedium()
        }
    }
    
    private fun vibrateTick() {
        if (!isSupported()) return
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            vibrator.vibrate(effect)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val effect = VibrationEffect.createOneShot(5, 50)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(5)
        }
    }
}
