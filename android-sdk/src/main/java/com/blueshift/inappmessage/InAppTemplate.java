package com.blueshift.inappmessage;

public enum InAppTemplate {
    HTML,
    CENTER_POPUP,
    FULL_SCREEN_POPUP,
    SLIDE_IN_BANNER;

    public static InAppTemplate fromString(String name) {
        if (name != null) {
            switch (name) {
                case "html":
                    return HTML;
                case "center_popup":
                    return CENTER_POPUP;
                case "full_screen_popup":
                    return FULL_SCREEN_POPUP;
                case "slide_in_banner":
                    return SLIDE_IN_BANNER;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        switch (this) {
            case HTML:
                return "html";
            case CENTER_POPUP:
                return "center_popup";
            case FULL_SCREEN_POPUP:
                return "full_screen_popup";
            case SLIDE_IN_BANNER:
                return "slide_in_banner";
        }

        return super.toString();
    }
}
