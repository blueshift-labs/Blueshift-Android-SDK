package com.blueshift.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Typeface
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
import com.blueshift.BlueshiftImageCache
import com.blueshift.inappmessage.InAppConstants
import com.blueshift.inappmessage.InAppMessage
import com.blueshift.inappmessage.InAppMessageIconFont
import com.blueshift.util.InAppUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Compose utility functions to replace traditional Android drawable-based styling
 * with Compose modifiers for in-app messages.
 */
internal object InAppComposeUtils {

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
        // Get background color and radius (equivalent to getTemplateBackgroundDrawable)
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

        //TODO Bg task
        var backgroundBitmap by remember { mutableStateOf<Bitmap?>(null) }
        LaunchedEffect(backgroundImageUrl) {
            if (!TextUtils.isEmpty(backgroundImageUrl)) {
                withContext(Dispatchers.IO) {
                    try {
                        val bitmap = BlueshiftImageCache.getBitmap(context, backgroundImageUrl)
                        backgroundBitmap = bitmap
                    } catch (e: Exception) {
                        // Handle image loading error silently
                        backgroundBitmap = null
                    }
                }
            }
        }

        // Create the layered container (matches View implementation exactly)
        Box(
            modifier = if (radiusDp > 0) {
                val shape = RoundedCornerShape(radiusDp.dp)
                Modifier
                    .fillMaxWidth()
                    .background(color = backgroundColor, shape = shape)
                    .clip(shape) // Equivalent to setClipToOutline(true)
            } else {
                Modifier.fillMaxWidth().background(color = backgroundColor)
            }
        ) {
            // Background image layer (equivalent to addBackgroundImageView)
            backgroundBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Content layer on top (equivalent to addView(childView))
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
            // Calculate all styling properties (equivalent to setContentTextView)
            val (textColor, textSize, textAlign, styledModifier) = remember(inAppMessage, contentName) {
                // TEXT COLOR
                val colorString = InAppUtils.getContentColor(context, inAppMessage, contentName)
                val color = if (InAppUtils.validateColorString(colorString)) {
                    parseColor(colorString)
                } else {
                    Color.Unspecified
                }
                
                // TEXT SIZE (default 14sp)
                val size = InAppUtils.getContentSize(context, inAppMessage, contentName, 14)
                
                // TEXT ALIGNMENT (convert Android Gravity to Compose TextAlign)
                val gravity = InAppUtils.getContentGravity(context, inAppMessage, contentName)
                val align = when (gravity) {
                    android.view.Gravity.START -> TextAlign.Start
                    android.view.Gravity.END -> TextAlign.End
                    android.view.Gravity.CENTER -> TextAlign.Center
                    android.view.Gravity.TOP -> TextAlign.Start // Top gravity doesn't apply to text alignment
                    android.view.Gravity.BOTTOM -> TextAlign.Start // Bottom gravity doesn't apply to text alignment
                    else -> TextAlign.Start
                }
                
                // BACKGROUND and PADDING
                val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, contentName)
                val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, contentName, 0)
                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                
                var textModifier = modifier
                
                // Apply background if specified
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    textModifier = if (bgRadius > 0) {
                        textModifier.background(color = bgColor, shape = RoundedCornerShape(bgRadius.dp))
                    } else {
                        textModifier.background(color = bgColor)
                    }
                }
                
                // Apply padding (default 4dp if not specified, matching View implementation)
                textModifier = if (padding != null) {
                    textModifier.padding(
                        start = padding.left.dp,
                        top = padding.top.dp,
                        end = padding.right.dp,
                        bottom = padding.bottom.dp
                    )
                } else {
                    textModifier.padding(4.dp) // Default padding from View implementation
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
    
    // Helper data class for multiple return values
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
        //TODO Bg task
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
                
                // Padding (e.g., "icon_padding": {"left": 4, "top": 4, "right": 4, "bottom": 4})
                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                var textModifier = modifier.fillMaxHeight()
                // Apply background if specified
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    textModifier = if (bgRadius > 0) {
                        textModifier.background(color = bgColor, shape = RoundedCornerShape(bgRadius.dp))
                    } else {
                        textModifier.background(color = bgColor)
                    }
                }

                // Build combined modifier with vertical center alignment
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
                        // Handle image loading error silently
                        imageBitmap = null
                    }
                }
            }

            // Calculate styling properties (equivalent to createContentIconView)
            val (containerModifier, imageModifier) = remember(inAppMessage, contentName) {
                // IMAGE BACKGROUND (looks at "icon_image_background_color" and "icon_image_background_radius")
                val bgColorString = InAppUtils.getContentBackgroundColor(context, inAppMessage, contentName)
                val bgRadius = InAppUtils.getContentBackgroundRadius(context, inAppMessage, contentName, 0)
                
                // CONTAINER PADDING
                val padding = InAppUtils.getContentPadding(context, inAppMessage, contentName)
                
                // Build container modifier (LinearLayout equivalent)
                var containerMod = modifier
                
                // CONTAINER BACKGROUND COLOR (looks at "icon_image_background_color")
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    containerMod = containerMod.background(color = bgColor)
                }
                
                // CONTAINER PADDING
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
                
                // Build image modifier (ImageView equivalent)
                var imageMod = Modifier.fillMaxSize()
                
                // IMAGE BACKGROUND
                if (InAppUtils.validateColorString(bgColorString)) {
                    val bgColor = parseColor(bgColorString)
                    imageMod = if (bgRadius > 0) {
                        val shape = RoundedCornerShape(bgRadius.dp)
                        imageMod
                            .background(color = bgColor, shape = shape)
                            .clip(shape) // Equivalent to setClipToOutline(true)
                    } else {
                        imageMod.background(color = bgColor)
                    }
                }
                
                Pair(containerMod, imageMod)
            }

            imageBitmap?.let { bitmap ->
                // Container view (LinearLayout equivalent with gravity CENTER)
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
}