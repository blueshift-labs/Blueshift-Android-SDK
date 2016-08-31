package com.blueshift.batch;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by rahul on 26/8/16.
 */
public class BulkEvent {
    ArrayList<HashMap<String,Object>> events;

    public void setEvents(ArrayList<HashMap<String,Object>> events) {
        this.events = events;
    }
}
