package com.blueshift.inappmessage;

public enum InAppTemplate {
    HTML;

    public static InAppTemplate fromString(String name) {
        if (name != null) {
            switch (name) {
                case "html":
                    return HTML;
            }
        }

        return null;
    }
}
