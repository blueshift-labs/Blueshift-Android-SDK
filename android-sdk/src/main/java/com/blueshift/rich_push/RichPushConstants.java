package com.blueshift.rich_push;

import android.content.Context;

/**
 * @author Rahul Raveendran V P
 *         Created on 25/2/15 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public final class RichPushConstants {
    public static final String DEFAULT_CHANNEL_ID = "bsft_channel_General";
    public static final String DEFAULT_CHANNEL_NAME = "General";
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";
    public static final String EXTRA_CAROUSEL_INDEX = "carousel_index";
    public static final String EXTRA_CAROUSEL_ELEMENT = "carousel_element";
    public static final String EXTRA_DEEP_LINK_URL = "deep_link_url";

    /**
     * Actions for the push categories handled by SDK.
     */
    private static final String sActionView = "ACTION_VIEW";
    private static final String sActionBuy = "ACTION_BUY";
    private static final String sActionOpenCart = "ACTION_OPEN_CART";
    private static final String sActionOpenOfferPage = "ACTION_OPEN_OFFER_PAGE";
    private static final String sActionOpenApp = "ACTION_OPEN_APP";

    public static String ACTION_VIEW(Context context) {
        return context.getPackageName() + "." + sActionView;
    }

    public static String ACTION_BUY(Context context) {
        return context.getPackageName() + "." + sActionBuy;
    }

    public static String ACTION_OPEN_CART(Context context) {
        return context.getPackageName() + "." + sActionOpenCart;
    }

    public static String ACTION_OPEN_OFFER_PAGE(Context context) {
        return context.getPackageName() + "." + sActionOpenOfferPage;
    }

    public static String ACTION_OPEN_APP(Context context) {
        return context.getPackageName() + "." + sActionOpenApp;
    }

    public static String buildAction(Context context, String action) {
        if (context == null) return null;

        if (action == null) return ACTION_OPEN_APP(context);

        switch (action) {
            case sActionView:
                return ACTION_VIEW(context);

            case sActionBuy:
                return ACTION_BUY(context);

            case sActionOpenCart:
                return ACTION_OPEN_CART(context);

            case sActionOpenOfferPage:
                return ACTION_OPEN_OFFER_PAGE(context);

            case sActionOpenApp:
                return ACTION_OPEN_APP(context);

            default:
                return ACTION_OPEN_APP(context);
        }
    }

    /**
     * Action sent to host app for handling the push message at the user end.
     */
    public static String ACTION_PUSH_RECEIVED(Context context) {
        return context.getPackageName() + ".ACTION_PUSH_RECEIVED";
    }
}
