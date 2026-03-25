package com.blueshift.compose.inapp

import android.content.Context
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.blueshift.BlueshiftConstants
import com.blueshift.BlueshiftLogger
import com.blueshift.compose.util.InAppComposeUtils
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.util.CommonUtils
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Compose overlay for banner in-app messages that positions the banner correctly
 * and handles background interactions like the View implementation.
 * Respects system UI (status bar, navigation bar) to show only in app content area.
 * Supports both obstrusive (modal) and unobstrusive (non-modal) modes based on enable_background_action.
 */
@Composable
internal fun InAppBannerOverlay(
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

@Composable
private fun InAppBanner(inAppMessage: InAppMessage, onDismiss: (() -> Unit)) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val activity = LocalActivity.current
    val coroutineScope = rememberCoroutineScope()
    
    // Calculate template margins exactly like InAppModal does
    val templateMargins = remember(inAppMessage) {
        val margins = inAppMessage.getTemplateMargin(context)
        if (margins != null) {
            val density = context.resources.displayMetrics.density
            android.graphics.Rect(
                (CommonUtils.dpToPx(margins.left, context) / density).toInt(),
                (CommonUtils.dpToPx(margins.top, context) / density).toInt(),
                (CommonUtils.dpToPx(margins.right, context) / density).toInt(),
                (CommonUtils.dpToPx(margins.bottom, context) / density).toInt()
            )
        } else {
            android.graphics.Rect(0, 0, 0, 0)
        }
    }
    
    val slideInOffsetX = remember { Animatable(-1f) }
    var isAnimating by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        slideInOffsetX.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 700)
        )
    }
    
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    
    val actionJson = remember(inAppMessage) {
        val actions = inAppMessage.actionsJSONArray
        if (actions != null && actions.length() > 0) {
            try {
                actions.getJSONObject(0)
            } catch (e: JSONException) {
                null
            }
        } else null
    }
    
    fun handleTapAction() {
        if (isAnimating) return
        isAnimating = true
        
        coroutineScope.launch {
            // Animate the entire banner out to the right
            slideInOffsetX.animateTo(
                targetValue = 1f, // Move to right side of screen
                animationSpec = tween(durationMillis = 1000)
            )
            
            // Execute action after animation
            if (actionJson != null) {
                executeAction(activity ?: context, inAppMessage, actionJson)
            }
            onDismiss()
        }
    }
    
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = templateMargins.left.dp,
                top = templateMargins.top.dp,
                end = templateMargins.right.dp,
                bottom = templateMargins.bottom.dp
            )
            .offset {
                val offsetXPx = (slideInOffsetX.value * screenWidthPx).roundToInt()
                IntOffset(offsetXPx, 0)
            }
    ) {
        InAppComposeUtils.InAppContainer(
            context = context,
            inAppMessage = inAppMessage
        ) {
            BannerContent(
                context = context,
                inAppMessage = inAppMessage,
                onDismiss = onDismiss,
                onTapAction = ::handleTapAction
            )
        }
    }
}

/**
 * Creates the banner content layout that matches InAppMessageViewBanner.getView()
 * Layout: [Icon/IconImage] [Message Text (weight=1)] [Secondary Icon]
 * Includes gesture handling and animations matching the View implementation
 */
@Composable
private fun BannerContent(
    context: Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit),
    onTapAction: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    fun handleSwipeDismiss() {
        val json = JSONObject()
        try {
            json.put(BlueshiftConstants.KEY_CLICK_ELEMENT, InAppConstants.ACT_SWIPE)
        } catch (ignored: JSONException) {
        }
        BlueshiftLogger.d("InAppBanner", "handleSwipeDismiss: ${inAppMessage.messageUuid}")
        InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
        onDismiss.invoke()
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        val threshold = 150f
                        val currentOffset = offsetX.value
                        when {
                            currentOffset > threshold -> {
                                handleSwipeDismiss()
                            }

                            currentOffset < -threshold -> {
                                handleSwipeDismiss()
                            }

                            else -> {
                                if (!isAnimating) {
                                    coroutineScope.launch {
                                        offsetX.animateTo(
                                            0f,
                                            animationSpec = tween(durationMillis = 300)
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) { _, dragAmount ->
                    if (!isAnimating) {
                        coroutineScope.launch {
                            // Reduce drag sensitivity to make it smoother and more controlled
                            val dampedDrag = dragAmount.x * 0.6f // Reduce sensitivity by 40%
                            offsetX.snapTo(offsetX.value + dampedDrag)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        onTapAction()
                    }
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        InAppComposeUtils.ContentIconText(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.ICON,
            modifier = Modifier
                .width(48.dp)
                .heightIn(max = 48.dp)
        )
        InAppComposeUtils.ContentIconImage(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.ICON_IMAGE,
            modifier = Modifier
                .width(48.dp)
                .heightIn(max = 48.dp)
        )
        InAppComposeUtils.ContentText(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.MESSAGE,
            modifier = Modifier.weight(1f)
        )
        InAppComposeUtils.ContentIconText(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.SECONDARY_ICON,
            modifier = Modifier
                .width(48.dp)
                .heightIn(max = 48.dp)
        )
    }
}

/**
 * Executes action functionality matching InAppMessageView.getActionClickListener()
 */
private fun executeAction(
    context: Context,
    inAppMessage: InAppMessage,
    actionJson: JSONObject
) {
    val actionName = actionJson.optString(InAppConstants.ACTION_TYPE)
    val statsParams = JSONObject()
    try {
        val androidLink = actionJson.optString(InAppConstants.ANDROID_LINK)
        if (!InAppUtils.isDismissUrl(androidLink)) {
            statsParams.put(BlueshiftConstants.KEY_CLICK_URL, androidLink)
        }
    } catch (ignored: JSONException) {
    }
    if (InAppManager.getActionCallback() != null) {
        val actionArgs = InAppComposeUtils.getActionArgsFromActionJson(actionJson)
        val callback = InAppManager.getActionCallback()
        callback?.onAction(actionName, actionArgs)
        InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
    } else {
        when (actionName) {
            InAppConstants.ACTION_OPEN -> {
                InAppComposeUtils.open(actionJson, context)
                val link = actionJson.optString(InAppConstants.ANDROID_LINK)
                if (InAppUtils.isDismissUrl(link)) {
                    InAppUtils.invokeInAppDismiss(context, inAppMessage, statsParams)
                } else {
                    InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
                }
            }
            InAppConstants.ACTION_SHARE -> {
                InAppComposeUtils.shareText(context, actionJson)
                InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
            }
            InAppConstants.ACTION_RATE_APP -> {
                InAppComposeUtils.rateAppInGooglePlayStore(context, actionJson)
                InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
            }
            else -> {
                InAppUtils.invokeInAppDismiss(context, inAppMessage, statsParams)
            }
        }
    }
}