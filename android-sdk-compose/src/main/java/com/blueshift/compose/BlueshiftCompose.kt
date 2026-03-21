package com.blueshift.compose

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.blueshift.BlueshiftImageCache
import com.blueshift.BlueshiftInAppListener
import com.blueshift.BlueshiftLogger
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppTemplate
import com.blueshift.util.CommonUtils
import com.blueshift.util.InAppUtils

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
                    InAppTemplate.RATING -> {
                        renderRatingInApp(inAppMessage, activity)
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
                            dismiss(this@apply, rootView)
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
                            dismiss(this@apply, rootView)
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
                            dismiss(this@apply, rootView)
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

    private fun renderRatingInApp(inAppMessage: InAppMessage, activity: Activity?): Boolean {
        return false
    }

    private fun dismiss(current: ViewGroup, rootView: ViewGroup){
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
    
    // Calculate template dimensions exactly like View implementation
    val templateDimensions = remember(inAppMessage) {
        calculateTemplateDimensions(context, inAppMessage)
    }
    
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(gravity)
            // Set window layout exactly like View implementation
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
    
    // Calculate template dimensions exactly like View implementation
    val templateDimensions = remember(inAppMessage) {
        calculateTemplateDimensions(context, inAppMessage)
    }
    
    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            dismissOnClickOutside = false,
            dismissOnBackPress = true,
            usePlatformDefaultWidth = false
        )
    ) {
        val dialogWindowProvider = LocalView.current.parent as? DialogWindowProvider
        dialogWindowProvider?.window?.let { window ->
            window.setGravity(gravity)
            // Set window layout exactly like View implementation
            window.setLayout(templateDimensions.width, templateDimensions.height)
        }
        InAppModal(inAppMessage, onDismiss)
    }
}

/**
 * Data class to hold template dimensions
 */
internal data class TemplateDimensions(
    val width: Int,
    val height: Int
)

/**
 * Calculates template dimensions exactly like the View implementation's prepareTemplateSize function
 */
internal fun calculateTemplateDimensions(context: Context, inAppMessage: InAppMessage): TemplateDimensions {
    // Calculate the space consumed by the status bar
    val topMargin = (24 * context.resources.displayMetrics.density).toInt()
    
    val maxWidth = context.resources.displayMetrics.widthPixels.toDouble()
    val maxHeight = (context.resources.displayMetrics.heightPixels - topMargin).toDouble()
    
    // Read the width % and height % available in the payload
    val widthPercentage = InAppUtils.getTemplateStyleInt(context, inAppMessage, InAppConstants.WIDTH, -1)
    val heightPercentage = InAppUtils.getTemplateStyleInt(context, inAppMessage, InAppConstants.HEIGHT, -1)
    
    val url = InAppUtils.getTemplateStyleString(context, inAppMessage, InAppConstants.BACKGROUND_IMAGE)
    val bitmap = if (url != null) BlueshiftImageCache.getBitmap(context, url) else null
    
    var width: Int
    var height: Int
    
    if (bitmap != null) {
        // We have a background image! Calculate dimensions based on aspect ratio
        val iWidth = bitmap.width.toDouble()
        val iHeight = bitmap.height.toDouble()
        val iRatio = iHeight / iWidth
        
        if (widthPercentage < 0 && heightPercentage < 0) {
            // No dimension provided in the template
            val adjustedWidthPercentage = 90 // MAX_WIDTH_PERCENTAGE
            val adjustedHeightPercentage = 90 // MAX_HEIGHT_PERCENTAGE
            
            val adjustedMaxWidth = (maxWidth * adjustedWidthPercentage) / 100
            val adjustedMaxHeight = (maxHeight * adjustedHeightPercentage) / 100
            
            val iWidthPx = CommonUtils.dpToPx(iWidth.toInt(), context)
            val iHeightPx = CommonUtils.dpToPx(iHeight.toInt(), context)
            
            if (iWidthPx < adjustedMaxWidth && iHeightPx < adjustedMaxHeight) {
                width = iWidthPx
                height = iHeightPx
            } else {
                width = adjustedMaxWidth.toInt()
                height = (adjustedMaxWidth * iRatio).toInt()
                
                if (height > adjustedMaxHeight.toInt()) {
                    width = (adjustedMaxHeight / iRatio).toInt()
                    height = adjustedMaxHeight.toInt()
                }
            }
        } else {
            // We have dimensions provided in the payload
            if (widthPercentage > 0) {
                val adjustedMaxHeight = if (widthPercentage != 100) {
                    (maxHeight * 90) / 100 // MAX_HEIGHT_PERCENTAGE
                } else {
                    maxHeight
                }
                
                val adjustedMaxWidth = (maxWidth * widthPercentage) / 100
                
                width = adjustedMaxWidth.toInt()
                height = (width.toDouble() * iRatio).toInt()
                
                if (height > adjustedMaxHeight.toInt()) {
                    width = (adjustedMaxHeight / iRatio).toInt()
                    height = adjustedMaxHeight.toInt()
                }
            } else {
                // heightPercentage > 0 and widthPercentage = -1 (auto)
                val adjustedMaxHeight = (maxHeight * heightPercentage) / 100
                val adjustedMaxWidth = if (heightPercentage != 100) {
                    (maxWidth * 90) / 100 // MAX_WIDTH_PERCENTAGE
                } else {
                    maxWidth
                }
                
                height = adjustedMaxHeight.toInt()
                width = (height.toDouble() / iRatio).toInt()
                
                if (width > adjustedMaxWidth.toInt()) {
                    width = adjustedMaxWidth.toInt()
                    height = (width.toDouble() * iRatio).toInt()
                }
            }
        }
    } else {
        // No background image - this is the key case for "min" settings
        width = if (widthPercentage < 0) {
            -2 // ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            ((maxWidth * widthPercentage) / 100).toInt()
        }
        
        height = if (heightPercentage < 0) {
            -2 // ViewGroup.LayoutParams.WRAP_CONTENT
        } else {
            ((maxHeight * heightPercentage) / 100).toInt()
        }
    }
    
    return TemplateDimensions(width, height)
}