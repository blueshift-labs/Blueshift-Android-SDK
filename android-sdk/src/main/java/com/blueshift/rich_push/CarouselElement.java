package com.blueshift.rich_push;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;

/**
 * This model represents the carousel element in carousel type notifications.
 *
 * @author Rahul Raveendran V P
 *         Created on 16/9/16 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public class CarouselElement implements Serializable {

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
}
