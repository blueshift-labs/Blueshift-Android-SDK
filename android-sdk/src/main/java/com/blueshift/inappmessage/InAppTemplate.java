package com.blueshift.inappmessage;

public enum InAppTemplate {
    HTML,
    CENTER_POPUP;

    public static InAppTemplate fromString(String name) {
        if (name != null) {
            switch (name) {
                case "html":
                    return HTML;
                case "center_popup":
                    return CENTER_POPUP;
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
        }

        return super.toString();
    }
}
