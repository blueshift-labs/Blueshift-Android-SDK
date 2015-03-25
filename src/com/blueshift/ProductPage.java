package com.blueshift;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Created by rahul on 19/2/15.
 *
 * This class is used for testing the SDK. Will be removed in the end.
 */
public class ProductPage extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String string = "SKU: " + getIntent().getStringExtra("sku")
                + "MRP: " + getIntent().getStringExtra("mrp")
                + "PRICE: " + getIntent().getStringExtra("price");

        Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
    }
}