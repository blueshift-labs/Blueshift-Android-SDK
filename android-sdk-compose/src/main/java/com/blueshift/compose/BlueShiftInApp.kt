package com.blueshift.compose

import android.app.Activity
import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.blueshift.BlueshiftExecutor
import com.blueshift.BlueshiftInAppListener
import com.blueshift.BlueshiftLogger
import com.blueshift.compose.inapp.InAppBannerOverlay
import com.blueshift.compose.inapp.InAppHtmlOverlay
import com.blueshift.compose.inapp.InAppModalOverlay
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppTemplate

/**
 * Pure Compose API for displaying in-app messages.
 * Add this composable to your Compose screens to enable in-app message display.
 * This renders in-app messages as native Compose components within your Compose tree.
 *
 * @param screenName Optional screen name for targeting. If null, uses Activity class name.
 *
 * Example usage:
 * ```
 * @Composable
 * fun MyApp() {
 *     BlueShiftInApp()
 *
 *     NavHost(...) {
 *         composable("home") {
 *             BlueShiftInApp("home")
 *             HomeScreen()
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun BlueShiftInApp(screenName: String? = null) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    var currentInApp by remember { mutableStateOf<InAppMessage?>(null) }

    LaunchedEffect(screenName, activity) {
        activity?.let {
            val finalScreenName = screenName ?: it.localClassName
            BlueshiftLogger.d(TAG, "registered for inApp: $finalScreenName")
            // InAppManager.registerForInAppMessages automatically handles cleanup of previous activity
            InAppManager.registerForInAppMessages(it, finalScreenName)
        }
    }

    // Lifecycle-aware listener management
    // Set listener when composable becomes visible, remove when invisible
    DisposableEffect(screenName, activity) {
        val finalScreenName = screenName ?: activity?.localClassName ?: "unknown"
        
        BlueshiftLogger.d(TAG, "listener added for screen: $finalScreenName")
        InAppManager.inAppRenderListener(finalScreenName, object : BlueshiftInAppListener {
            override fun onInAppDelivered(attributes: MutableMap<String, Any>?) {}
            override fun onInAppOpened(attributes: MutableMap<String, Any>?) {}
            override fun onInAppClicked(attributes: MutableMap<String, Any>?) {}

            override fun renderInApp(inAppMessage: InAppMessage?, activity: Activity?): Boolean {
                BlueshiftLogger.d(TAG, "received inApp to render: ${inAppMessage?.messageUuid} for screen: $finalScreenName")
                if(currentInApp == null) {
                    currentInApp = inAppMessage
                    return true
                }
                return false
            }
        })

        onDispose {
            currentInApp = null
            BlueshiftLogger.d(TAG, "listener removed for screen: $finalScreenName")
            InAppManager.removeInAppRenderListener(finalScreenName)
        }
    }

    currentInApp?.let { inApp ->
        when (inApp.template) {
            InAppTemplate.MODAL -> {
                InAppModalOverlay(
                    inAppMessage = inApp,
                    onDismiss = {
                        currentInApp = null
                        performInAppCleanup(inApp, context)
                    }
                )
            }
            InAppTemplate.HTML -> {
                InAppHtmlOverlay(
                    inAppMessage = inApp,
                    onDismiss = {
                        currentInApp = null
                        performInAppCleanup(inApp, context)
                    }
                )
            }
            InAppTemplate.SLIDE_IN_BANNER -> {
                InAppBannerOverlay(
                    inAppMessage = inApp,
                    onDismiss = {
                        currentInApp = null
                        performInAppCleanup(inApp, context)
                    }
                )
            }
            else -> {
                currentInApp = null
            }
        }
    }
}

/**
 * Performs cleanup operations when in-app is dismissed
 */
private fun performInAppCleanup(inAppMessage: InAppMessage, context: Context) {
    BlueshiftExecutor.getInstance().runOnDiskIOThread {
        InAppManager.clearCachedAssets(inAppMessage, context)
        InAppManager.scheduleNextInAppMessage(context)
        InAppManager.cleanUpOngoingInAppCache()
    }
}

private val TAG = "BlueShiftInApp"