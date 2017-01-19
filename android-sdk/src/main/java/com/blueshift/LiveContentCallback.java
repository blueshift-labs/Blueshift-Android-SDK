package com.blueshift;

/**
 * This interface provides a callback method 'onReceive()' which is
 * used by the live content API inside Blueshift sdk. It is responsible
 * for delivering the api response received from the server to the caller.
 *
 * @author Rahul Raveendran V P
 *         Created on 13/1/17 @ 3:16 PM
 *         https://github.com/rahulrvp
 */

public interface LiveContentCallback {
    void onReceive(String response);
}
