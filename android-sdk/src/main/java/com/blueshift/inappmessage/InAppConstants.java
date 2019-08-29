package com.blueshift.inappmessage;

@SuppressWarnings("WeakerAccess")
public class InAppConstants {
    // local constants
    public static final long IN_APP_INTERVAL = 1000 * 60 * 5; // 5 minutes

    // events
    public static final String EVENT_DELIVERED = "delivered";
    public static final String EVENT_VIEW = "view";
    public static final String EVENT_CLICK = "click";
    public static final String EVENT_EXTRA_ELEMENT = "element";

    // actions
    public static final String ACTION_DISMISS = "dismiss";
    public static final String ACTION_OPEN = "open";
    public static final String ACTION_SHARE = "share";
    public static final String ACTION_RATE_APP = "rate_app";
    public static final String ACTION_CLOSE = "close"; // to track close button clicks

    // payload: level-1 keys
    public static final String TYPE = "type";
    public static final String TRIGGER = "trigger";
    public static final String DISPLAY_ON = "display_on";
    public static final String EXPIRES_AT = "expires_at";
    public static final String CONTENT = "content";
    public static final String CONTENT_STYLE = "content_style";
    public static final String TEMPLATE_STYLE = "template_style";

    // payload: level-2 keys
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String POSITION = "position";
    public static final String FULLSCREEN = "fullscreen";

    public static final String TEXT = "text";
    public static final String ACTIONS = "actions";
    public static final String ACTION_TYPE = "type";
    public static final String HTML = "html";
    public static final String MESSAGE = "message";
    public static final String BANNER = "banner";
    public static final String ICON = "icon";
    public static final String TITLE = "title";
    public static final String PAGE = "page";
    public static final String EXTRAS = "extras";
    public static final String SHAREABLE_TEXT = "shareable_text";

    public static final String ORIENTATION = "orientation";
    public static final String GRAVITY = "gravity";
    public static final String LAYOUT_GRAVITY = "layout_gravity";
    public static final String SIZE = "size";
    public static final String COLOR = "color";
    public static final String BACKGROUND_COLOR = "background_color";
    public static final String BACKGROUND_RADIUS = "background_radius";
    public static final String MARGIN = "margin";
    public static final String PADDING = "padding";
    public static final String CLOSE_BUTTON = "close_button";

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
