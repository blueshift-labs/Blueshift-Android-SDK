package com.blueshift.rich_push;

import java.io.Serializable;

/**
 * Created by rahul on 16/9/16.
 */
public class CarouselElement implements Serializable {
    private String image_url;
    private String action;

    public String getImageUrl() {
        return image_url;
    }

    public String getAction() {
        return action;
    }
}
