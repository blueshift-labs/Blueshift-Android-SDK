package com.blueshift.test_app;

import android.app.Application;

/**
 * Created by rahul on 19/2/15.
 */
public class BFApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * Test code for initialization
         */

        /*
        Configuration configuration = new Configuration();
        configuration.setAppIcon(R.drawable.ic_launcher);
        configuration.setProductPage(ProductPage.class);
        configuration.setCartPage(ProductPage.class);
        configuration.setOfferDisplayPage(ProductPage.class);
        configuration.setApiKey("5be04919d8773728197f8bd0e2fedce2");

        UserInfo userInfo = UserInfo.getInstance(this);
        userInfo.setRetailerCustomerId("909090");
        userInfo.setDateOfBirth(18,1,1989);

        Blueshift.getInstance(this).initialize(configuration);
        */
    }
}
