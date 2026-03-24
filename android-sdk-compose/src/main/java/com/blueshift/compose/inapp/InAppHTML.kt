package com.blueshift.compose.inapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.blueshift.Blueshift
import com.blueshift.BlueshiftConstants
import com.blueshift.BlueshiftLogger
import com.blueshift.compose.util.InAppComposeUtils
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppMessageIconFont
import com.blueshift.util.BlueshiftUtils
import com.blueshift.util.CommonUtils
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File

@Composable
internal fun InAppHTML(inAppMessage: InAppMessage, onDismiss: (() -> Unit)) {
    val context = LocalContext.current
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
            android.graphics.Rect(16, 16, 16, 16)
        }
    }
    // Calculate template dimensions exactly like View implementation
    val templateDimensions = remember(inAppMessage) {
        InAppComposeUtils.calculateTemplateDimensions(context, inAppMessage)
    }

    val density = LocalDensity.current

    // Convert template dimensions to Compose constraints
    val widthModifier = if (templateDimensions.width == -2) { // WRAP_CONTENT
        Modifier.wrapContentWidth()
    } else {
        val adjustedWidth = templateDimensions.width - (templateMargins.left + templateMargins.right)
        if (adjustedWidth > 0) {
            Modifier.width(with(density) { adjustedWidth.toDp() })
        } else {
            Modifier.wrapContentWidth()
        }
    }

    val heightModifier = if (templateDimensions.height == -2) { // WRAP_CONTENT
        Modifier.wrapContentHeight()
    } else {
        val adjustedHeight = templateDimensions.height - (templateMargins.top + templateMargins.bottom)
        if (adjustedHeight > 0) {
            Modifier.height(with(density) { adjustedHeight.toDp() })
        } else {
            Modifier.wrapContentHeight()
        }
    }
    
    Box(
        modifier = Modifier
            .then(widthModifier)
            .then(heightModifier)
            .padding(
                start = templateMargins.left.dp,
                top = templateMargins.top.dp,
                end = templateMargins.right.dp,
                bottom = templateMargins.bottom.dp
            )
    ) {
        InAppComposeUtils.InAppContainer(
            context = context,
            inAppMessage = inAppMessage
        ) {
            HTMLContent(
                context = context,
                inAppMessage = inAppMessage,
                onDismiss = onDismiss
            )
        }
        val enableClose = InAppUtils.isHTML(inAppMessage)
        val showCloseButton = InAppUtils.shouldShowCloseButton(context, inAppMessage, enableClose)
        if (showCloseButton) {
            CloseButton(
                context = context,
                inAppMessage = inAppMessage,
                onDismiss = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Creates the HTML content using WebView wrapped in AndroidView.
 * This replicates the functionality from InAppMessageViewHTML.getView()
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun HTMLContent(
    context: Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit)
) {
    val htmlContent = remember(inAppMessage) {
        inAppMessage.getContentString(InAppConstants.HTML)
    }
    val localActivity = LocalActivity.current
    val webViewClient = remember(inAppMessage, onDismiss) {
        InAppWebViewClient(localActivity ?: context, inAppMessage, onDismiss)
    }
    
    if (!TextUtils.isEmpty(htmlContent)) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    val config = BlueshiftUtils.getConfiguration(ctx)
                    if (config != null && config.isJavaScriptForInAppWebViewEnabled()) {
                        settings.javaScriptEnabled = true
                    }
                    setWebViewClient(webViewClient)
                    val width = getWebViewDimension(ctx, inAppMessage, InAppConstants.WIDTH)
                    val height = getWebViewDimension(ctx, inAppMessage, InAppConstants.HEIGHT)
                    layoutParams = ViewGroup.LayoutParams(width, height)
                    loadData(CommonUtils.getBase64(htmlContent), "text/html; charset=UTF-8", "base64")
                }
            }
        )
    }
}

/**
 * Decides what should be the dimension of the WebView based on the availability
 * of height and width. Replicates InAppMessageViewHTML.getWebViewDimension()
 */
private fun getWebViewDimension(context: Context, inAppMessage: InAppMessage, dimensionName: String): Int {
    val dimension = InAppUtils.getTemplateStyleInt(context, inAppMessage, dimensionName, -1)
    return if (dimension > 0) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
}

/**
 * Custom WebViewClient that handles URL loading and actions.
 * Replicates the functionality from InAppMessageViewHTML.InAppWebViewClient
 */
private class InAppWebViewClient(
    private val context: Context,
    private val inAppMessage: InAppMessage,
    private val onDismiss: (() -> Unit)
) : WebViewClient() {
    
    companion object {
        private const val TAG = "InAppWebViewClient"
    }
    
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val uri = request.url
                launchUri(uri, dismiss = onDismiss)
            } catch (e: Exception) {
                handleClick(inAppMessage, getClickStatsJSONObject())
            }
            true
        } else {
            super.shouldOverrideUrlLoading(view, request)
        }
    }
    
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            launchUri(uri, onDismiss)
            true
        } catch (e: Exception) {
            handleClick(inAppMessage, getClickStatsJSONObject())
            true
        }
    }
    
    private fun launchUri(uri: Uri, dismiss: () -> Unit) {
        when {
            InAppUtils.isDismissUri(uri) -> invokeDismiss(uri)
            InAppUtils.isAskPNPermissionUri(uri) -> invokeNotificationPermissionReq(uri)
            else -> {
                val actionCallback = InAppManager.getActionCallback()
                if (actionCallback != null) {
                    val link = uri.toString()
                    
                    val statsParams = getClickStatsJSONObject()
                    try {
                        if (!TextUtils.isEmpty(link)) {
                            statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, link)
                        }
                    } catch (ignore: JSONException) {
                    }
                    
                    val jsonObject = JSONObject()
                    try {
                        jsonObject.put(InAppConstants.ANDROID_LINK, link)
                    } catch (ignored: JSONException) {
                    }
                    
                    actionCallback.onAction(InAppConstants.ACTION_OPEN, jsonObject)
                    handleClick(inAppMessage, statsParams)
                } else {
                    openUri(uri)
                }
            }
        }
        dismiss()
    }
    
    private fun openUri(uri: Uri) {
        when {
            InAppUtils.isDismissUri(uri) -> invokeDismiss(uri)
            InAppUtils.isAskPNPermissionUri(uri) -> invokeNotificationPermissionReq(uri)
            else -> {
                try {
                    if (context is Activity) {
                        BlueshiftUtils.openURL(
                            uri.toString(),
                            context,
                            android.os.Bundle(),
                            BlueshiftConstants.LINK_SOURCE_INAPP
                        )
                    }
                } catch (e: Exception) {
                    BlueshiftLogger.e(TAG, e)
                }
                
                val statsParams = getClickStatsJSONObject()
                try {
                    val link = uri.toString()
                    if (!TextUtils.isEmpty(link)) {
                        statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, link)
                    }
                } catch (ignored: JSONException) {
                }
                
                handleClick(inAppMessage, statsParams)
            }
        }
    }
    
    private fun invokeNotificationPermissionReq(uri: Uri) {
        Blueshift.requestPushNotificationPermission(context)
        
        val statsParams = getClickStatsJSONObject()
        try {
            val url = uri.toString()
            if (!TextUtils.isEmpty(url)) {
                statsParams.putOpt(BlueshiftConstants.KEY_CLICK_URL, url)
            }
        } catch (ignored: Exception) {
        }
        
        handleClick(inAppMessage, statsParams)
    }
    
    private fun invokeDismiss(uri: Uri) {
        val json = if (InAppUtils.isBlueshiftDismissUrl(uri.toString())) {
            JSONObject().apply {
                try {
                    put(BlueshiftConstants.KEY_CLICK_URL, uri.toString())
                } catch (ignored: JSONException) {
                }
            }
        } else null
        InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
        onDismiss?.invoke()
    }
    
    private fun handleClick(inAppMessage: InAppMessage, statsParams: JSONObject) {
        InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
    }
    
    private fun getClickStatsJSONObject(): JSONObject {
        return JSONObject()
    }
}

/**
 * Close button composable that replicates InAppMessageView.addCloseButton() functionality.
 * Positioned in top-right corner with proper styling and click handling.
 */
@Composable
private fun CloseButton(
    context: Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val iconHeight = remember(inAppMessage) {
        InAppUtils.getCloseButtonIconTextSize(context, inAppMessage) + 8
    }
    val (iconText, textColor, backgroundColor) = remember(inAppMessage) {
        val defaultText = "\uF00D"
        val defaultColor = "#ffffff"
        val defaultBgColor = "#3f3f44"
        try {
            val closeButtonString = InAppUtils.getTemplateStyleString(context, inAppMessage, InAppConstants.CLOSE_BUTTON)
            if (!closeButtonString.isNullOrEmpty()) {
                val closeButtonJson = JSONObject(closeButtonString)
                val glyph = InAppUtils.getStringFromJSONObject(closeButtonJson, InAppConstants.TEXT)
                val finalText = if (glyph.isNullOrEmpty()) defaultText else glyph
                val colorString = InAppUtils.getStringFromJSONObject(closeButtonJson, InAppConstants.TEXT_COLOR)
                val finalTextColor = if (InAppUtils.validateColorString(colorString)) colorString else defaultColor
                val bgColorString = InAppUtils.getStringFromJSONObject(closeButtonJson, InAppConstants.BACKGROUND_COLOR)
                val finalBgColor = if (InAppUtils.validateColorString(bgColorString)) bgColorString else defaultBgColor
                
                Triple(finalText, finalTextColor, finalBgColor)
            } else {
                Triple(defaultText, defaultColor, defaultBgColor)
            }
        } catch (e: Exception) {
            Triple(defaultText, defaultColor, defaultBgColor)
        }
    }
    var fontFamily by remember { mutableStateOf<FontFamily?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val iconFontInstance = InAppMessageIconFont.getInstance(context)
                val fontFile = File(context.filesDir, "Font+Awesome+5+Free-Solid-900.otf")
                
                if (fontFile.exists()) {
                    val typeface = Typeface.createFromFile(fontFile)
                    fontFamily = FontFamily(androidx.compose.ui.text.font.Typeface(typeface))
                } else {
                    iconFontInstance.updateFont(context)
                }
            } catch (e: Exception) {
                fontFamily = null
            }
        }
    }
    
    Box(
        modifier = modifier
            .size(iconHeight.dp)
            .offset(x = (-8).dp, y = 8.dp)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                val statsParams = JSONObject()
                try {
                    statsParams.put(BlueshiftConstants.KEY_CLICK_ELEMENT, InAppConstants.BTN_CLOSE)
                } catch (ignored: JSONException) {
                }
                InAppUtils.invokeInAppDismiss(context, inAppMessage, statsParams)
                onDismiss?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(iconHeight.dp)
                .background(
                    color = Color(android.graphics.Color.parseColor(backgroundColor)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = iconText,
                fontSize = InAppUtils.getCloseButtonIconTextSize(context, inAppMessage).sp,
                color = Color(android.graphics.Color.parseColor(textColor)),
                textAlign = TextAlign.Center,
                fontFamily = fontFamily ?: FontFamily.Default
            )
        }
    }
}
