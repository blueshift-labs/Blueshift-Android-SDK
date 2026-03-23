package com.blueshift.compose

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueshift.Blueshift
import com.blueshift.BlueshiftConstants
import com.blueshift.BlueshiftImageCache
import com.blueshift.BlueshiftLogger
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppMessageIconFont
import com.blueshift.util.BlueshiftUtils
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File

/**
 * Compose utility functions to replace traditional Android drawable-based styling
 * with Compose modifiers for in-app messages.
 */
internal object InAppComposeUtils {

    private const val TAG = "InAppComposeUtils"

    /**
     * Creates a complete in-app message container that matches the View implementation exactly.
     * This combines background color/radius (GradientDrawable) + background image + content layering.
     *
     * Replicates the exact layering from InAppMessageView constructor:
     * 1. Container background (GradientDrawable with color/radius)
     * 2. Background image (ImageView as child)
     * 3. Content on top
     *
     * @param context Android context
     * @param inAppMessage The in-app message containing style configuration
     * @param content The content to be displayed on top
     */
    @Composable
    internal fun InAppContainer(
        context: Context,
        inAppMessage: InAppMessage,
        content: @Composable () -> Unit
    ) {
        val colorString = InAppUtils.getTemplateStyleString(
            context,
            inAppMessage,
            InAppConstants.BACKGROUND_COLOR
        )
        val backgroundColor = if (InAppUtils.validateColorString(colorString)) {
            parseColor(colorString!!)
        } else {
            Color.White
        }

        val radiusDp = InAppUtils.getTemplateStyleInt(
            context,
            inAppMessage,
            InAppConstants.BACKGROUND_RADIUS,
            0
        )

        val backgroundImageUrl = InAppUtils.getTemplateStyleString(
            context,
            inAppMessage,
            InAppConstants.BACKGROUND_IMAGE
        )

        var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(backgroundImageUrl) {
            if (!TextUtils.isEmpty(backgroundImageUrl)) {
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = BlueshiftImageCache.getBitmap(context, backgroundImageUrl)
                        backgroundBitmap = bitmap
                    } catch (e: Exception) {
                        backgroundBitmap = null
                    }
                }
            }
        }
        Box(
            modifier = if (radiusDp > 0) {
                val shape = RoundedCornerShape(radiusDp.dp)
                Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor, shape = shape)
                    .clip(shape)
            } else {
                Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor)
            }
        ) {
            backgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            content()
        }
    }

    /**
     * Creates a content text composable that matches getContentTextView() functionality.
     * Equivalent to InAppMessageView.getContentTextView()
     *
     * Replicates the exact styling from InAppUtils.setContentTextView():
     * - Text content from inAppMessage.getContentString()
     * - Text color from InAppUtils.getContentColor()
     * - Text size from InAppUtils.getContentSize() (default 14sp)
     * - Text alignment from InAppUtils.getContentGravity()
     * - Background from InAppUtils.getContentBackgroundDrawable()
     * - Padding from InAppUtils.getContentPadding()
     */
    @Composable
    internal fun ContentText(
        context: Context,
        inAppMessage: InAppMessage,
        contentName: String,
        modifier: Modifier = Modifier
    ) {
        val textContent = inAppMessage.getContentString(contentName)
        if (!TextUtils.isEmpty(textContent)) {
            val (textColor, textSize, textAlign, styledModifier) = remember(inAppMessage, contentName) {
                val colorString = InAppUtils.getContentColor(context, inAppMessage, contentName)
                val color = if (InAppUtils.validateColorString(colorString)) {
                    parseColor(colorString)
                } else {
                    Color.Unspecified
                }
                val size = InAppUtils.getContentSize(context, inAppMessage, contentName, 14)
                val gravity = InAppUtils.getContentGravity(context, inAppMessage, contentName)
                val align = when (gravity) {
                    android.view.Gravity.START -> TextAlign.Start
                    android.view.Gravity.END -> TextAlign.End
                    android.view.Gravity.CENTER -> TextAlign.Center
                    android.view.Gravity.TOP -> TextAlign.Start
                    android.view.Gravity.BOTTOM -> TextAlign.Start
                    else -> TextAlign.Start
                }

                val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, contentName)
                val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, contentName, 0)
                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                
                var textModifier = modifier

                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    textModifier = if (bgRadius > 0) {
                        textModifier.background(color = bgColor, shape = RoundedCornerShape(bgRadius.dp))
                    } else {
                        textModifier.background(color = bgColor)
                    }
                }
                textModifier = if (padding != null) {
                    textModifier.padding(
                        start = padding.left.dp,
                        top = padding.top.dp,
                        end = padding.right.dp,
                        bottom = padding.bottom.dp
                    )
                } else {
                    textModifier.padding(4.dp)
                }
                
                Tuple4(color, size, align, textModifier)
            }
            
            Text(
                text = textContent,
                modifier = styledModifier,
                color = textColor,
                fontSize = textSize.sp,
                textAlign = textAlign
            )
        }
    }

    private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    /**
     * Creates a content icon text composable that matches getContentIconTextView() functionality.
     * Equivalent to InAppMessageView.getContentIconTextView()
     *
     * This function:
     * 1. Gets the icon text (unicode) from the message content
     * 2. Applies Font Awesome font using InAppMessageIconFont
     * 3. Renders the unicode as Font Awesome icons
     */
    @Composable
    internal fun ContentIconText(
        context: Context,
        inAppMessage: InAppMessage,
        contentName: String,
        modifier: Modifier = Modifier
    ) {
        val iconText = inAppMessage.getContentString(contentName)
        if (!TextUtils.isEmpty(iconText)) {
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
            val (iconColor, iconSize, styledModifier) = remember(inAppMessage, contentName) {
                val colorString = InAppUtils.getContentColor(context, inAppMessage, contentName)
                val color = if (InAppUtils.validateColorString(colorString)) {
                    parseColor(colorString)
                } else {
                    Color.Unspecified
                }
                val size = InAppUtils.getContentSize(context, inAppMessage, contentName, 14)
                val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, contentName)
                val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, contentName, 0)

                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                var textModifier = modifier.fillMaxHeight()
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    textModifier = if (bgRadius > 0) {
                        textModifier.background(color = bgColor, shape = RoundedCornerShape(bgRadius.dp))
                    } else {
                        textModifier.background(color = bgColor)
                    }
                }

                textModifier = if (padding != null) {
                    textModifier.padding(
                        start = padding.left.dp,
                        top = padding.top.dp,
                        end = padding.right.dp,
                        bottom = padding.bottom.dp
                    )
                } else {
                    textModifier.padding(4.dp)
                }
                
                Triple(color, size, textModifier)
            }
            Text(
                text = iconText,
                modifier = styledModifier.wrapContentHeight(align = Alignment.CenterVertically),
                fontFamily = fontFamily ?: FontFamily.Default,
                color = iconColor,
                fontSize = iconSize.sp,
                textAlign = TextAlign.Center,
            )
        }
    }

    /**
     * Creates a content icon image composable that matches createContentIconView() functionality.
     * Equivalent to InAppUtils.createContentIconView()
     *
     * Replicates the exact styling from InAppUtils.createContentIconView():
     * - Image loading from BlueshiftImageCache
     * - Background color from InAppUtils.getContentBackgroundColor()
     * - Background radius from InAppUtils.getContentBackgroundRadius()
     * - Container padding from InAppUtils.getContentPadding()
     * - Container background color (same as image background)
     * - Clip to outline for rounded corners
     */
    @Composable
    internal fun ContentIconImage(
        context: Context,
        inAppMessage: InAppMessage,
        contentName: String,
        modifier: Modifier = Modifier
    ) {
        val imageUrl = inAppMessage.getContentString(contentName)
        if (!TextUtils.isEmpty(imageUrl)) {
            var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
            
            LaunchedEffect(imageUrl) {
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = BlueshiftImageCache.getBitmap(context, imageUrl)
                        imageBitmap = bitmap
                    } catch (e: Exception) {
                        imageBitmap = null
                    }
                }
            }

            val (containerModifier, imageModifier) = remember(inAppMessage, contentName) {
                val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, contentName)
                val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, contentName, 0)
                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                var containerMod = modifier
                
                // Apply background color and roundness to the container (which includes padding)
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    containerMod = if (bgRadius > 0) {
                        val shape = RoundedCornerShape(bgRadius.dp)
                        containerMod.background(color = bgColor, shape = shape)
                    } else {
                        containerMod.background(color = bgColor)
                    }
                }

                containerMod = if (padding != null) {
                    containerMod.padding(
                        start = padding.left.dp,
                        top = padding.top.dp,
                        end = padding.right.dp,
                        bottom = padding.bottom.dp
                    )
                } else {
                    containerMod
                }

                // Image should just fill the available space and clip to the same shape if rounded
                var imageMod = Modifier.fillMaxSize()
                if (bgRadius > 0) {
                    val shape = RoundedCornerShape(bgRadius.dp)
                    imageMod = imageMod.clip(shape)
                }
                
                Pair(containerMod, imageMod)
            }

            imageBitmap?.let { bitmap ->
                Box(
                    modifier = containerModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = imageModifier,
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }

    /**
     * Parses a color string (e.g., "#FFFFFF", "#FFF") to Compose Color.
     *
     * @param colorString The color string to parse
     * @return Compose Color object
     * @throws IllegalArgumentException if the color string is invalid
     */
    private fun parseColor(colorString: String): Color {
        return try {
            Color(android.graphics.Color.parseColor(colorString))
        } catch (e: IllegalArgumentException) {
            Color.White
        }
    }

    fun bannerDismissedWhenClickedOutside(inAppMessage: InAppMessage, context: Context) {
        val json = JSONObject()
        try {
            json.put(
                BlueshiftConstants.KEY_CLICK_ELEMENT,
                InAppConstants.ACT_TAP_OUTSIDE
            )
        } catch (ignored: JSONException) {
        }
        InAppUtils.invokeInAppDismiss(context, inAppMessage, json)
    }

    fun getActionArgsFromActionJson(actionJson: JSONObject?): JSONObject? {
        try {
            if (actionJson != null) {
                val actionName = actionJson.optString(InAppConstants.ACTION_TYPE)
                val actionArgs = JSONObject()

                when (actionName) {
                    InAppConstants.ACTION_OPEN -> {
                        val link = actionJson.optString(InAppConstants.ANDROID_LINK)
                        actionArgs.put(InAppConstants.ANDROID_LINK, link)
                    }

                    InAppConstants.ACTION_SHARE -> {
                        val shareContent = actionJson.optString(InAppConstants.SHAREABLE_TEXT)
                        actionArgs.put(InAppConstants.SHAREABLE_TEXT, shareContent)
                    }
                }

                val extras = actionJson.optJSONObject(InAppConstants.EXTRAS)
                actionArgs.put(InAppConstants.EXTRAS, extras)

                return actionArgs
            }
        } catch (e: java.lang.Exception) {
            BlueshiftLogger.e(TAG, e)
        }
        return null
    }

    fun open(action: JSONObject?, context: Context) {
        try {
            if (action != null) {
                val link = action.optString(InAppConstants.ANDROID_LINK)
                BlueshiftLogger.d(
                    TAG,
                    "android_link: $link"
                )

                if (InAppUtils.isDismissUrl(link)) {
                    BlueshiftLogger.d(TAG, "Dismiss URL detected.")
                } else if (InAppUtils.isAskPNPermissionUri(Uri.parse(link))) {
                    Blueshift.requestPushNotificationPermission(context)
                } else {
                    val extras = action.optJSONObject(InAppConstants.EXTRAS)
                    var bundle: Bundle? = null
                    if (extras != null) {
                        bundle = Bundle()
                        val keys = extras.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val `val` = extras.optString(key)
                            bundle.putString(key, `val`)
                        }
                    }

                    try {
                        if (context is Activity) {
                            BlueshiftUtils.openURL(
                                link,
                                context,
                                bundle,
                                BlueshiftConstants.LINK_SOURCE_INAPP
                            )
                        }
                    } catch (e: java.lang.Exception) {
                        BlueshiftLogger.e(TAG, e)
                        try {
                            val clazz = Class.forName(link)
                            val intent = Intent(context, clazz)
                            if (bundle != null) {
                                intent.putExtras(bundle)
                            }
                            context.startActivity(intent)
                        } catch (ex: Exception) {
                            BlueshiftLogger.e(TAG, ex)
                        }
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            BlueshiftLogger.e(TAG, e)
        }
    }

    fun shareText(context: Context, action: JSONObject?) {
        try {
            if (action != null) {
                val text = action.optString(InAppConstants.SHAREABLE_TEXT)
                if (!TextUtils.isEmpty(text)) {
                    val shareIntent = Intent(Intent.ACTION_SEND)
                    shareIntent.putExtra(Intent.EXTRA_TEXT, text)
                    shareIntent.setType("text/plain")
                    context.startActivity(shareIntent)
                }
            }
        } catch (e: java.lang.Exception) {
            BlueshiftLogger.e(TAG, e)
        }
    }

    fun rateAppInGooglePlayStore(context: Context, action: JSONObject) {
        try {
            val pkgName: String = context.packageName
            try {
                val marketUri = Uri.parse("market://details?id=$pkgName")
                val marketIntent = Intent(Intent.ACTION_VIEW, marketUri)
                context.startActivity(marketIntent)
            } catch (e: java.lang.Exception) {
                BlueshiftLogger.e(TAG, e)

                try {
                    val marketWebUri =
                        Uri.parse("https://play.google.com/store/apps/details?id=$pkgName")
                    val marketWebIntent = Intent(Intent.ACTION_VIEW, marketWebUri)
                    context.startActivity(marketWebIntent)
                } catch (ex: java.lang.Exception) {
                    BlueshiftLogger.e(TAG, ex)
                }
            }
        } catch (e: java.lang.Exception) {
            BlueshiftLogger.e(TAG, e)
        }
    }
}