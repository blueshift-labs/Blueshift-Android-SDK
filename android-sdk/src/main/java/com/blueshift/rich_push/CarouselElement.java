package com.blueshift.rich_push;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 16/9/16 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public class CarouselElement implements Serializable {
    private String image_url;
    private String action;
    private String deep_link_url;
    private HashMap data;

    public String getImageUrl() {
        return image_url;
    }

    public String getAction() {
        return action;
    }

    public String getDeepLinkUrl() {
        return deep_link_url;
    }

    public HashMap getData() {
        return data;
    }

    public boolean isDeepLinkingEnabled() {
        return !TextUtils.isEmpty(deep_link_url);
    }
}
