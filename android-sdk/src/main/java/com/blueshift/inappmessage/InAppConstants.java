package com.blueshift.inappmessage;

@SuppressWarnings("WeakerAccess")
public class InAppConstants {
    // local constants
    public static final long IN_APP_INTERVAL = 60000; // (1000 * 60)ms, ie; 1 minute

    public static final String DISMISS_URL = "blueshift://dismiss";
    public static final String BLANK_URL = "about:blank#blocked";
    public static final String ACT_BACK = "btn_back";
    public static final String ACT_SWIPE = "swipe";
    public static final String ACT_TAP_OUTSIDE = "tap_outside";

    // events
    public static final String EVENT_DELIVERED = "delivered";
    public static final String EVENT_OPEN = "open";
    public static final String EVENT_CLICK = "click";
    public static final String EVENT_DISMISS = "dismiss";
    public static final String EVENT_EXTRA_ELEMENT = "element";

    // actions
    public static final String ACTION_DISMISS = "dismiss";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_RATE_APP = "rate_app";

    public static final String BTN_CLOSE = "btn_close"; // to track close button clicks

    // payload: level-1 keys
    public static final String TYPE = "type";
    public static final String TRIGGER = "trigger";
    public static final String DISPLAY_ON = "display_on_android";
    public static final String EXPIRES_AT = "expires_at";
    public static final String CONTENT = "content";
    public static final String CONTENT_STYLE = "content_style";
    public static final String CONTENT_STYLE_DARK = "content_style_dark";
    public static final String TEMPLATE_STYLE = "template_style";
    public static final String TEMPLATE_STYLE_DARK = "template_style_dark";

    // payload: level-2 keys
    public static final String CANCEL_ON_TOUCH_OUTSIDE = "cancel_on_touch_outside";
    public static final String ENABLE_BACKGROUND_ACTION = "enable_background_action";
    public static final String WIDTH = "width";
    public static final String BACKGROUND_DIM_AMOUNT = "background_dim_amount";
    public static final String HEIGHT = "height";
    public static final String POSITION = "position";
    public static final String FULLSCREEN = "fullscreen";

    public static final String TEXT = "text";
    public static final String TEXT_COLOR = "text_color";
    public static final String TEXT_SIZE = "text_size";
    public static final String ACTIONS = "actions";
    public static final String ACTION_TYPE = "type";
    public static final String HTML = "html";
    public static final String MESSAGE = "message";
    public static final String BANNER = "banner";
    public static final String ICON = "icon";
    public static final String ICON_IMAGE = "icon_image";
    public static final String SECONDARY_ICON = "secondary_icon";
    public static final String TITLE = "title";
    public static final String ANDROID_LINK = "android_link";
    public static final String EXTRAS = "extras";
    public static final String SHAREABLE_TEXT = "shareable_text";

    public static final String ORIENTATION = "orientation";
    public static final String GRAVITY = "gravity";
    public static final String LAYOUT_GRAVITY = "layout_gravity";
    public static final String SIZE = "size";
    public static final String COLOR = "color";
    public static final String BACKGROUND_COLOR = "background_color";
    public static final String BACKGROUND_IMAGE = "background_image";
    public static final String BACKGROUND_RADIUS = "background_radius";
    public static final String MARGIN = "margin";
    public static final String PADDING = "padding";
    public static final String CLOSE_BUTTON = "close_button";
    public static final String CLOSE_BUTTON_SHOW = "show";

    private static String append(String prefix, String suffix) {
        return prefix != null && suffix != null ? prefix + "_" + suffix : null;
    }

    public static String ORIENTATION(String prefix) {
        return append(prefix, ORIENTATION);
    }

    public static String GRAVITY(String prefix) {
        return append(prefix, GRAVITY);
    }

    public static String LAYOUT_GRAVITY(String prefix) {
        return append(prefix, LAYOUT_GRAVITY);
    }

    public static String SIZE(String prefix) {
        return append(prefix, SIZE);
    }

    public static String COLOR(String prefix) {
        return append(prefix, COLOR);
    }

    public static String BACKGROUND_COLOR(String prefix) {
        return append(prefix, BACKGROUND_COLOR);
    }

    public static String BACKGROUND_RADIUS(String prefix) {
        return append(prefix, BACKGROUND_RADIUS);
    }

    public static String MARGIN(String prefix) {
        return append(prefix, MARGIN);
    }

    public static String PADDING(String prefix) {
        return append(prefix, PADDING);
    }
}
