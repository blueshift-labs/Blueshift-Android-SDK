package com.blueshift.compose

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.blueshift.BlueshiftConstants
import com.blueshift.BlueshiftExecutor
import com.blueshift.BlueshiftInAppListener
import com.blueshift.BlueshiftLogger
import com.blueshift.compose.inapp.InAppBanner
import com.blueshift.compose.inapp.InAppHTML
import com.blueshift.compose.inapp.InAppModal
import com.blueshift.compose.util.InAppComposeUtils
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppTemplate
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

object BlueshiftCompose {

    private const val TAG = "BlueshiftCompose"

    fun addInAppRenderer() {
        InAppManager.inAppRenderListener(object : BlueshiftInAppListener {
            override fun onInAppDelivered(attributes: MutableMap<String, Any>?) {}

            override fun onInAppOpened(attributes: MutableMap<String, Any>?) {}

            override fun onInAppClicked(attributes: MutableMap<String, Any>?) {}

            override fun renderInApp(inAppMessage: InAppMessage?, activity: Activity?): Boolean {
                return when (inAppMessage?.template) {
                    InAppTemplate.SLIDE_IN_BANNER -> {
                        renderBannerInApp(inAppMessage, activity)
                    }
                    InAppTemplate.MODAL -> {
                        renderModalInApp(inAppMessage, activity)
                    }
                    InAppTemplate.HTML -> {
                        renderHtmlInApp(inAppMessage, activity)
                    }
                    else -> false
                }
            }
        })
    }

    private fun renderBannerInApp(inAppMessage: InAppMessage, activity: Activity?): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return false
        return try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = ComposeView(activity).apply {
                setContent {
                    InAppBannerOverlay(
                        inAppMessage = inAppMessage,
                        onDismiss = {
                            dismiss(this@apply, rootView, inAppMessage, activity.applicationContext)
                        }
                    )
                }
            }
            rootView.addView(composeView)
            true
        } catch (e: Exception) {
            BlueshiftLogger.e(TAG, "error while rendering: ${e.message}")
            true
        }
    }

    private fun renderModalInApp(inAppMessage: InAppMessage, activity: Activity?): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return false
        return try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = ComposeView(activity).apply {
                setContent {
                    InAppModalOverlay(
                        inAppMessage = inAppMessage,
                        onDismiss = {
                            dismiss(this@apply, rootView, inAppMessage, activity.applicationContext)
                        }
                    )
                }
            }
            rootView.addView(composeView)
            true
        } catch (e: Exception) {
            BlueshiftLogger.e(TAG, "Error while rendering Modal in-app: ${e.message}")
            false
        }
    }

    private fun renderHtmlInApp(inAppMessage: InAppMessage, activity: Activity?): Boolean {
        if (activity == null || activity.isFinishing || activity.isDestroyed) return false
        return try {
            val rootView = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = ComposeView(activity).apply {
                setContent {
                    InAppHtmlOverlay(
                        inAppMessage = inAppMessage,
                        onDismiss = {
                            dismiss(this@apply, rootView, inAppMessage, activity.applicationContext)
                        }
                    )
                }
            }
            rootView.addView(composeView)
            true
        } catch (e: Exception) {
            BlueshiftLogger.e(TAG, "Error while rendering HTML in-app: ${e.message}")
            false
        }
    }

    private fun dismiss(current: ViewGroup, rootView: ViewGroup, inAppMessage: InAppMessage, context: Context){
        val safeRemoval = {
            try {
                if (current.parent == rootView) {
                    rootView.removeView(current)
                } else {
                    BlueshiftLogger.d(TAG, "Safe view removal failed")
                }
            } catch (e: Exception) {
                BlueshiftLogger.d(TAG, "Safe view removal: ${e.message}")
            }
        }
        BlueshiftExecutor.getInstance().runOnDiskIOThread {
            InAppManager.clearCachedAssets(inAppMessage, context)
            InAppManager.scheduleNextInAppMessage(context)
            InAppManager.cleanUpOngoingInAppCache()
        }
        
        if (Looper.myLooper() == Looper.getMainLooper()) {
            safeRemoval()
        } else {
            Handler(Looper.getMainLooper()).post {
                safeRemoval()
            }
        }
    }
}

/**
 * Compose overlay for banner in-app messages that positions the banner correctly
 * and handles background interactions like the View implementation.
 * Respects system UI (status bar, navigation bar) to show only in app content area.
 * Supports both obstrusive (modal) and unobstrusive (non-modal) modes based on enable_background_action.
 */
@Composable
private fun InAppBannerOverlay(
    inAppMessage: InAppMessage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val gravity = InAppUtils.getTemplateGravity(context, inAppMessage)
    val enableBackgroundActions = InAppUtils.shouldEnableBackgroundActions(context, inAppMessage)
    LaunchedEffect(inAppMessage) {
        withContext(Dispatchers.IO) {
            InAppManager.invokeOnInAppViewed(inAppMessage)
        }
    }
    
    Dialog(
        onDismissRequest = {
            InAppComposeUtils.bannerDismissedWhenClickedOutside(inAppMessage, context)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(gravity)

            if (enableBackgroundActions) {
                window.addFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL)
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            }
        }
        InAppBanner(inAppMessage, onDismiss)
    }
}

/**
 * Compose overlay for HTML in-app messages that positions the HTML content correctly
 * and handles background interactions like the View implementation.
 * Uses Dialog to create a modal overlay for HTML content that sizes to content.
 */
@Composable
private fun InAppHtmlOverlay(
    inAppMessage: InAppMessage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val gravity = InAppUtils.getTemplateGravity(context, inAppMessage)
    LaunchedEffect(inAppMessage) {
        withContext(Dispatchers.IO) {
            InAppManager.invokeOnInAppViewed(inAppMessage)
        }
    }
    val templateDimensions = remember(inAppMessage) {
        InAppComposeUtils.calculateTemplateDimensions(context, inAppMessage)
    }
    val cancelOnTouchOutside = remember(inAppMessage) {
        InAppUtils.shouldCancelOnTouchOutside(context, inAppMessage)
    }
    
    Dialog(
        onDismissRequest = {
            val json = JSONObject()
            try {
                json.put(BlueshiftConstants.KEY_CLICK_ELEMENT, InAppConstants.ACT_BACK)
            } catch (ignored: JSONException) {}
            InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = cancelOnTouchOutside,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(gravity)
            window.setLayout(templateDimensions.width, templateDimensions.height)
        }
        InAppHTML(inAppMessage, onDismiss)
    }
}

/**
 * Compose overlay for Modal in-app messages that positions the modal content correctly
 * and handles background interactions like the View implementation.
 * Uses Dialog to create a modal overlay for modal content.
 */
@Composable
private fun InAppModalOverlay(
    inAppMessage: InAppMessage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val gravity = InAppUtils.getTemplateGravity(context, inAppMessage)
    LaunchedEffect(inAppMessage) {
        withContext(Dispatchers.IO) {
            InAppManager.invokeOnInAppViewed(inAppMessage)
        }
    }
    val templateDimensions = remember(inAppMessage) {
        InAppComposeUtils.calculateTemplateDimensions(context, inAppMessage)
    }
    val cancelOnTouchOutside = remember(inAppMessage) {
        InAppUtils.shouldCancelOnTouchOutside(context, inAppMessage)
    }
    
    Dialog(
        onDismissRequest = {
            val json = JSONObject()
            try {
                json.put(BlueshiftConstants.KEY_CLICK_ELEMENT, InAppConstants.ACT_BACK)
            } catch (ignored: JSONException) {}
            InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = cancelOnTouchOutside,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(gravity)
            window.setLayout(templateDimensions.width, templateDimensions.height)
        }
        InAppModal(inAppMessage, onDismiss)
    }
}
