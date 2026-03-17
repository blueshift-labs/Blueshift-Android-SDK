package com.blueshift.compose

import android.content.Context
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blueshift.BlueshiftConstants
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppMessageIconFont
import com.blueshift.util.CommonUtils
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.File

@Composable
internal fun InAppModal(inAppMessage: InAppMessage, onDismiss: (() -> Unit)? = null) {
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

    val maxWidth = with(LocalDensity.current) {
        (context.resources.displayMetrics.widthPixels * 0.9f).toDp()
    }
    val maxHeight = with(LocalDensity.current) {
        (context.resources.displayMetrics.heightPixels * 0.8f).toDp()
    }
    
    Box(
        modifier = Modifier
            .wrapContentSize()
            .widthIn(max = maxWidth)
            .heightIn(max = maxHeight)
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
            ModalContent(
                context = context,
                inAppMessage = inAppMessage,
                onDismiss = onDismiss
            )
        }

        val enableClose = InAppUtils.isModalWithNoActionButtons(inAppMessage)
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
 * Creates the modal content layout that matches InAppMessageViewModal.getView()
 * Layout: [Banner Image] [Title] [Message] [Action Buttons]
 */
@Composable
private fun ModalContent(
    context: Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit)? = null
) {
    val (heightAvailable, backgroundImageAvailable, fillParent) = remember(inAppMessage) {
        val heightSet = InAppUtils.isHeightSet(context, inAppMessage)
        val bgImageUrl = InAppUtils.getTemplateStyleString(context, inAppMessage, InAppConstants.BACKGROUND_IMAGE)
        val bgImageAvailable = !bgImageUrl.isNullOrEmpty()
        val shouldFillParent = heightSet || bgImageAvailable
        Triple(heightSet, bgImageAvailable, shouldFillParent)
    }
    
    Column(
        modifier = if (fillParent) {
            Modifier.fillMaxHeight()
        } else {
            Modifier.wrapContentHeight()
        }
    ) {
        InAppComposeUtils.ContentIconImage(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.BANNER,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
        )

        val titleLayoutGravity = remember(inAppMessage) {
            InAppUtils.getContentLayoutGravity(context, inAppMessage, InAppConstants.TITLE)
        }
        val titleAlignment = when (titleLayoutGravity) {
            android.view.Gravity.START -> Alignment.Start
            android.view.Gravity.END -> Alignment.End
            android.view.Gravity.CENTER -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
        
        InAppComposeUtils.ContentText(
            context = context,
            inAppMessage = inAppMessage,
            contentName = InAppConstants.TITLE,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(align = titleAlignment)
        )

        val messageLayoutGravity = remember(inAppMessage) {
            InAppUtils.getContentLayoutGravity(context, inAppMessage, InAppConstants.MESSAGE)
        }
        val messageAlignment = when (messageLayoutGravity) {
            android.view.Gravity.START -> Alignment.Start
            android.view.Gravity.END -> Alignment.End
            android.view.Gravity.CENTER -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
        
        if (fillParent) {
            InAppComposeUtils.ContentText(
                context = context,
                inAppMessage = inAppMessage,
                contentName = InAppConstants.MESSAGE,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .wrapContentWidth(align = messageAlignment)
            )
        } else {
            InAppComposeUtils.ContentText(
                context = context,
                inAppMessage = inAppMessage,
                contentName = InAppConstants.MESSAGE,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentWidth(align = messageAlignment)
            )
        }

        ActionButtons(
            context = context,
            inAppMessage = inAppMessage,
            onDismiss = onDismiss
        )
    }
}

/**
 * Creates action buttons that replicate InAppMessageView.getActionButtons() functionality
 *
 * Replicates the exact styling from InAppMessageView.getActionButtons():
 * - Actions container background from InAppUtils.getContentBackgroundDrawable()
 * - Actions container padding from InAppUtils.getContentPadding()
 * - Actions container margins from InAppUtils.getContentMargin()
 * - Individual button margins from actionJson MARGIN property
 */
@Composable
private fun ActionButtons(
    context: Context,
    inAppMessage: InAppMessage,
    onDismiss: (() -> Unit)? = null
) {
    val actionsArray = inAppMessage.actionsJSONArray
    if (actionsArray != null && actionsArray.length() > 0) {
        val orientation = InAppUtils.getActionOrientation(context, inAppMessage)

        val (actionsContainerModifier, actionMargins) = remember(inAppMessage) {
            val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, InAppConstants.ACTIONS)
            val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, InAppConstants.ACTIONS, 0)

            val containerPadding = InAppUtils.getContentPadding(context, inAppMessage, InAppConstants.ACTIONS)

            val margins = InAppUtils.getContentMargin(context, inAppMessage, InAppConstants.ACTIONS)
            val actionMargins = if (margins != null) {
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
            
            var containerModifier = Modifier.fillMaxWidth()

            if (InAppUtils.validateColorString(bgColorString)) {
                val bgColor = Color(android.graphics.Color.parseColor(bgColorString))
                containerModifier = if (bgRadius > 0) {
                    containerModifier.background(color = bgColor, shape = RoundedCornerShape(bgRadius.dp))
                } else {
                    containerModifier.background(color = bgColor)
                }
            }

            containerModifier = if (containerPadding != null) {
                containerModifier.padding(
                    start = containerPadding.left.dp,
                    top = containerPadding.top.dp,
                    end = containerPadding.right.dp,
                    bottom = containerPadding.bottom.dp
                )
            } else {
                containerModifier
            }

            containerModifier = containerModifier.padding(
                start = actionMargins.left.dp,
                top = actionMargins.top.dp,
                end = actionMargins.right.dp,
                bottom = actionMargins.bottom.dp
            )
            
            Pair(containerModifier, actionMargins)
        }
        
        if (orientation == android.widget.LinearLayout.VERTICAL) {
            Column(
                modifier = actionsContainerModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                for (i in 0 until actionsArray.length()) {
                    val actionObject = actionsArray.getJSONObject(i)
                    ActionButton(
                        context = context,
                        inAppMessage = inAppMessage,
                        actionJson = actionObject,
                        element = BlueshiftConstants.BTN_(i),
                        onDismiss = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                modifier = actionsContainerModifier,
                horizontalArrangement = Arrangement.Center
            ) {
                for (i in 0 until actionsArray.length()) {
                    val actionObject = actionsArray.getJSONObject(i)
                    ActionButton(
                        context = context,
                        inAppMessage = inAppMessage,
                        actionJson = actionObject,
                        element = BlueshiftConstants.BTN_(i),
                        onDismiss = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Creates a single action button that replicates InAppMessageView.getActionButton() functionality
 */
@Composable
private fun ActionButton(
    context: Context,
    inAppMessage: InAppMessage,
    actionJson: JSONObject,
    element: String,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val buttonText = remember(actionJson) {
        InAppUtils.getStringFromJSONObject(actionJson, InAppConstants.TEXT) ?: ""
    }
    
    if (buttonText.isNotEmpty()) {
        val (textColor, textSize, textAlign, styledModifier) = remember(actionJson) {
            val colorString = InAppUtils.getStringFromJSONObject(actionJson, InAppConstants.COLOR(InAppConstants.TEXT))
            val color = if (InAppUtils.validateColorString(colorString)) {
                Color(android.graphics.Color.parseColor(colorString))
            } else {
                Color.White
            }

            val size = InAppUtils.getIntFromJSONObject(actionJson, InAppConstants.SIZE(InAppConstants.TEXT), 14)

            val gravity = InAppUtils.getIntFromJSONObject(actionJson, InAppConstants.GRAVITY(InAppConstants.TEXT), android.view.Gravity.CENTER)
            val align = when (gravity) {
                android.view.Gravity.START -> TextAlign.Start
                android.view.Gravity.END -> TextAlign.End
                android.view.Gravity.CENTER -> TextAlign.Center
                android.view.Gravity.TOP -> TextAlign.Center
                android.view.Gravity.BOTTOM -> TextAlign.Center
                else -> TextAlign.Center
            }

            val bgColor = InAppUtils.getActionBackgroundColor(actionJson)
            val bgRadius = InAppUtils.getActionBackgroundRadius(actionJson)

            val padding = InAppUtils.getRectFromJSONObject(actionJson, InAppConstants.PADDING)

            val margin = InAppUtils.getRectFromJSONObject(actionJson, InAppConstants.MARGIN)
            
            var buttonModifier = modifier
                .clickable {
                    handleActionClick(context, inAppMessage, actionJson, element, onDismiss)
                }

            buttonModifier = if (margin != null) {
                buttonModifier.padding(
                    start = margin.left.dp,
                    top = margin.top.dp,
                    end = margin.right.dp,
                    bottom = margin.bottom.dp
                )
            } else {
                buttonModifier
            }

            if (bgColor != 0) {
                val backgroundColor = Color(bgColor)
                buttonModifier = if (bgRadius > 0) {
                    buttonModifier.background(color = backgroundColor, shape = RoundedCornerShape(bgRadius.dp))
                } else {
                    buttonModifier.background(color = backgroundColor)
                }
            } else {
                buttonModifier = buttonModifier.background(
                    color = Color(android.graphics.Color.parseColor("#2196f3")),
                    shape = RoundedCornerShape(3.dp)
                )
            }

            buttonModifier = if (padding != null) {
                buttonModifier.padding(
                    start = padding.left.dp,
                    top = padding.top.dp,
                    end = padding.right.dp,
                    bottom = padding.bottom.dp
                )
            } else {
                buttonModifier.padding(12.dp)
            }
            
            Tuple4(color, size, align, buttonModifier)
        }
        
        Text(
            text = buttonText,
            modifier = styledModifier,
            color = textColor,
            fontSize = textSize.sp,
            textAlign = textAlign
        )
    }
}

private data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Handles action button clicks, replicating InAppMessageView.getActionClickListener() logic
 */
private fun handleActionClick(
    context: Context,
    inAppMessage: InAppMessage,
    actionJson: JSONObject,
    element: String,
    onDismiss: (() -> Unit)? = null
) {
    try {
        val actionName = actionJson.optString(InAppConstants.ACTION_TYPE)
        val statsParams = JSONObject()
        try {
            statsParams.put(BlueshiftConstants.KEY_CLICK_ELEMENT, element)
            val androidLink = actionJson.optString(InAppConstants.ANDROID_LINK)
            if (!InAppUtils.isDismissUrl(androidLink)) {
                statsParams.put(BlueshiftConstants.KEY_CLICK_URL, androidLink)
            }
        } catch (ignored: JSONException) {
        }
        when (actionName) {
            InAppConstants.ACTION_DISMISS -> {
                InAppUtils.invokeInAppDismiss(context, inAppMessage, statsParams)
                onDismiss?.invoke()
            }
            InAppConstants.ACTION_OPEN -> {
                InAppUtils.invokeInAppClicked(context, inAppMessage, statsParams)
                // TODO: Implement URL opening logic
            }
            else -> {
                InAppUtils.invokeInAppDismiss(context, inAppMessage, statsParams)
                onDismiss?.invoke()
            }
        }
    } catch (e: Exception) {
        InAppUtils.invokeInAppDismiss(context, inAppMessage, JSONObject())
        onDismiss?.invoke()
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