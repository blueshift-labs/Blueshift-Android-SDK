package com.blueshift.test_app;

import android.app.Activity;
import android.os.Bundle;
import com.blueshift.R;

/**
 * This class is used for testing the SDK. Will be removed in the end.
 */

public class LandingActivity extends Activity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    protected void onResume() {
        super.onResume();

        /*

        Blueshift.getInstance(this).trackScreenView(this);
        Blueshift.getInstance(this).trackEmailListSubscription("test_user@gmail.com");
        Blueshift.getInstance(this).trackEmailListUnsubscription("test_user@gmail.com");
        Blueshift.getInstance(this).trackProductView("E29090", 1);
        Blueshift.getInstance(this).trackAddToCart("E29090", 1);

        */

        /*

        Product[] products = new Product[10];
        for (int i = 1; i <= 10; i++) {
            Product product = new Product();
            product.setSku("P00" + i);
            product.setQuantity(i);
            //product.setPrice(1.0f * 10.50f);
            products[i - 1] = product;
        }

        //Blueshift.getInstance(this).trackCheckoutCart(products, 10.0f, 20.0f, "BLAH-01");
        //Blueshift.getInstance(this).trackProductsPurchase("100", products, 10.0f, 40.0f, 20.0f, "BLAH-01");
        Blueshift.getInstance(this).trackPurchaseCancel("100");
        Blueshift.getInstance(this).trackPurchaseReturn("100", products);

        */

    }
}
