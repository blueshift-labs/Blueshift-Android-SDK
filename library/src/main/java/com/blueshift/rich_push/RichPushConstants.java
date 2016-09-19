package com.blueshift.rich_push;

import android.content.Context;

/**
 * Created by rahul on 25/2/15.
 */
public final class RichPushConstants {
    public static final String EXTRA_MESSAGE = "message";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

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
        if (context == null || action == null) return null;

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
