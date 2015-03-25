package com.blueshift.rich_push;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by rahul on 18/2/15.
 */
public class Message implements Serializable {
    public final static String CATEGORY_BUY = "buy";
    public final static String CATEGORY_OFFER = "offer";
    public final static String CATEGORY_VIEW_CART = "view cart";

    String id;
    String type;
    String title;
    String body;
    String category;
    String sku;
    String mrp;
    String price;
    String image_url;
    long expiry;
    HashMap<String, Object> data;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getCategory() {
        return category;
    }

    public String getSku() {
        return sku;
    }

    public String getMrp() {
        return mrp;
    }

    public String getPrice() {
        return price;
    }

    public String getImage_url() {
        return image_url;
    }

    public long getExpiry() {
        return expiry;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
}
