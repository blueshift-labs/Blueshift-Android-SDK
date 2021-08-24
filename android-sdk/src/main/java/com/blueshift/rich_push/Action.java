package com.blueshift.rich_push;

import java.io.Serializable;

public class Action implements Serializable {
    private String title;
    private String deepLink;

    public String getTitle() {
        return title;
    }

    public String getDeepLink() {
        return deepLink;
    }
}
