package com.blueshift.test_app;

import android.content.Context;
import android.util.Log;
import com.blueshift.rich_push.Message;
import com.blueshift.rich_push.RichPushActionReceiver;

/**
 * This class is used for testing the SDK. Will be removed in the end.
 */

public class CustomPushReceiver extends RichPushActionReceiver {

    @Override
    public void displayProductPage(Context context, Message message) {
        /**
         * User's implementation of displayProductPage goes here
         * This allows the user to override the deep-linking actions.
         */

        Log.i("CustomPushReceiver", "Display product page was called.");
    }

    @Override
    public void displayCartPage(Context context, Message message) {
        super.displayCartPage(context, message);
    }

    @Override
    public void addToCart(Context context, Message message) {
        super.addToCart(context, message);
    }
}
