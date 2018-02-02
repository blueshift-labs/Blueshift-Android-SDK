package com.blueshift.rich_push;

import java.io.Serializable;

/**
 * @author Rahul Raveendran V P
 *         Created on 01/02/18 @ 8:27 PM
 *         https://github.com/rahulrvp
 */


public class CarouselElementText implements Serializable {
    private String text;
    private String text_color;
    private String text_background_color;
    private int text_size;

    public String getText() {
        return text;
    }

    public String getTextColor() {
        return text_color;
    }

    public int getTextSize() {
        return text_size;
    }

    public String getTextBackgroundColor() {
        return text_background_color;
    }
}
