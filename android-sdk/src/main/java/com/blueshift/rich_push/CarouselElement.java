package com.blueshift.rich_push;

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
    private HashMap data;

    public String getImageUrl() {
        return image_url;
    }

    public String getAction() {
        return action;
    }

    public HashMap getData() {
        return data;
    }
}
