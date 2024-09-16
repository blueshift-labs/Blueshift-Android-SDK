package com.blueshift.batch;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Rahul Raveendran V P
 *         Created on 26/8/16 @ 3:04 PM
 *         https://github.com/rahulrvp
 *
 * @deprecated
 * This class is deprecated and will be removed in a future release. The events module has been
 * refactored to improve performance and reliability. This class is now used internally for legacy
 * data migration and will not be supported going forward.
 */
@Deprecated
public class BulkEvent {
    ArrayList<HashMap<String,Object>> events;

    public void setEvents(ArrayList<HashMap<String,Object>> events) {
        this.events = events;
    }
}
