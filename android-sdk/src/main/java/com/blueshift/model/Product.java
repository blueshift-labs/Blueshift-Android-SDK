package com.blueshift.model;

/**
 * @author Rahul Raveendran V P
 *         Created on 5/3/15 @ 3:03 PM
 *         https://github.com/rahulrvp
 */
public class Product {
    private String sku;
    private int quantity;
    private float price;

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public float getPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}
