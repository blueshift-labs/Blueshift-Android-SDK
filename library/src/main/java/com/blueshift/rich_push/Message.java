package com.blueshift.rich_push;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by rahul on 18/2/15.
 */
public class Message implements Serializable {
    String id;
    String notification_type;
    String title;
    String body;
    String category;
    String sku;
    String mrp;
    String price;
    String url;
    String image_url;
    long expiry;
    HashMap<String, Object> data;

    public String getId() {
        return id;
    }

    public NotificationType getNotificationType() {
        return NotificationType.fromString(notification_type);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public NotificationCategory getCategory() {
        return NotificationCategory.fromString(category);
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

    public String getUrl() {
        return url;
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
