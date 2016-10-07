package com.blueshift.batch;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 26/8/16 @ 3:04 PM
 *         https://github.com/rahulrvp
 */
public class BulkEvent {
    ArrayList<HashMap<String,Object>> events;

    public void setEvents(ArrayList<HashMap<String,Object>> events) {
        this.events = events;
    }
}
