package com.blueshift.rich_push;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.blueshift.BlueshiftLogger;
import com.google.gson.Gson;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This model represents the carousel element in carousel type notifications.
 *
 * @author Rahul Raveendran V P
 * Created on 16/9/16 @ 3:03 PM
 * https://github.com/rahulrvp
 */
public class CarouselElement implements Serializable {
    private static final String TAG = "CarouselElement";

    /**
     * CarouselElementText to be shown on the carousel element
     */
    private CarouselElementText content_text;

    /**
     * ContentSubtext to be shown on the carousel element
     */
    private CarouselElementText content_subtext;

    /**
     * Overlay type
     */
    private String content_layout_type;

    /**
     * The image URL used for rendering image in Notification
     */
    private String image_url;

    /**
     * Optional action to perform when a carousel image is clicked. Supported values are,
     * - ACTION_OPEN_APP (Default)
     * - ACTION_VIEW
     * - ACTION_BUY
     * - ACTION_OPEN_CART
     * - ACTION_OPEN_OFFER_PAGE
     */
    private String action;

    /**
     * On clicking notification, app will be opened and this URL will be passed along with the
     * corresponding carousel element object to the launching activity of the app.
     * <p>
     * Note: If this key is present, it will override the above actions.
     */
    private String deep_link_url;

    /**
     * Optional additional parameters the developer can send.
     */
    private HashMap data;

    public String getImageUrl() {
        return image_url;
    }

    public String getAction() {
        return action;
    }

    public String getDeepLinkUrl() {
        return deep_link_url;
    }

    public HashMap getData() {
        return data;
    }

    public boolean isDeepLinkingEnabled() {
        return !TextUtils.isEmpty(deep_link_url);
    }

    public String getContentLayoutType() {
        return content_layout_type;
    }

    public CarouselElementText getContentText() {
        return content_text;
    }

    public CarouselElementText getContentSubtext() {
        return content_subtext;
    }

    public static CarouselElement parseIntent(Intent intent) {
        if (intent != null) {
            return parseBundle(intent.getExtras());
        }

        return null;
    }

    public static CarouselElement parseBundle(Bundle bundle) {
        if (bundle != null) {
            String json = bundle.getString(RichPushConstants.EXTRA_CAROUSEL_ELEMENT);
            return fromJson(json);
        }

        return null;
    }

    public static CarouselElement fromJson(String json) {
        try {
            return new Gson().fromJson(json, CarouselElement.class);
        } catch (Exception e) {
            BlueshiftLogger.e(TAG, e);
        }

        return null;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
