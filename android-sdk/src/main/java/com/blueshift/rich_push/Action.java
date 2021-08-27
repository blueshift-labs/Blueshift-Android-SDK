package com.blueshift.rich_push;

import java.io.Serializable;

public class Action implements Serializable {
    private String title;
    private String deep_link_url;

    public String getTitle() {
        return title;
    }

    public String getDeepLinkUrl() {
        return deep_link_url;
    }
}
