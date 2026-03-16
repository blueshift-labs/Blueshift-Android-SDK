package com.blueshift.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.blueshift.BlueshiftConstants
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

@Composable
internal fun InAppBanner(inAppMessage: InAppMessage, onDismiss: (() -> Unit)? = null) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val slideInOffsetX = remember { Animatable(-1f) }
    LaunchedEffect(Unit) {
        slideInOffsetX.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 700)
        )
    }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxWidth()
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
                onDismiss = onDismiss
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
    context: android.content.Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }

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

    fun handleSwipeDismiss() {
        val json = JSONObject()
        try {
            json.put(BlueshiftConstants.KEY_CLICK_ELEMENT, InAppConstants.ACT_SWIPE)
        } catch (ignored: JSONException) {
        }
        InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
        onDismiss?.invoke()
    }
    
    fun handleTapAction() {
        if (isAnimating) return
        isAnimating = true
        
        coroutineScope.launch {
            val bannerWidth = with(density) { 300.dp.toPx() }
            offsetX.animateTo(
                targetValue = bannerWidth,
                animationSpec = tween(durationMillis = 1000)
            )
            if (actionJson != null) {
                executeAction(context, inAppMessage, actionJson)
            }
        }
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
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
                                        offsetX.animateTo(0f, animationSpec = tween(durationMillis = 300))
                                    }
                                }
                            }
                        }
                    }
                ) { _, dragAmount ->
                    if (!isAnimating) {
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        handleTapAction()
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
            modifier = Modifier.width(48.dp)
        )
        InAppComposeUtils.ContentIconImage(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.ICON_IMAGE,
            modifier = Modifier.width(48.dp)
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
            modifier = Modifier.width(48.dp)
        )
    }
}

/**
 * Executes action functionality matching InAppMessageView.getActionClickListener()
 */
private fun executeAction(context: android.content.Context, inAppMessage: InAppMessage, actionJson: JSONObject) {
    val statsParams = JSONObject()
    try {
        val androidLink = actionJson.optString(InAppConstants.ANDROID_LINK)
        if (!InAppUtils.isDismissUrl(androidLink)) {
            statsParams.put(BlueshiftConstants.KEY_CLICK_URL, androidLink)
        }
    } catch (ignored: JSONException) {
    }
    InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
}