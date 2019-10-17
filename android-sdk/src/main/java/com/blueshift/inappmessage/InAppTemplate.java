package com.blueshift.inappmessage;

public enum InAppTemplate {
    HTML,
    MODAL,
    SLIDE_IN_BANNER,
    RATING;

    public static InAppTemplate fromString(String name) {
        if (name != null) {
            switch (name) {
                case "html":
                    return HTML;
                case "modal":
                    return MODAL;
                case "slide_in_banner":
                    return SLIDE_IN_BANNER;
                case "rating":
                    return RATING;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        switch (this) {
            case HTML:
                return "html";
            case MODAL:
                return "modal";
            case SLIDE_IN_BANNER:
                return "slide_in_banner";
            case RATING:
                return "rating";
        }

        return super.toString();
    }
}
