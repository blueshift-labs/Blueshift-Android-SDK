package com.blueshift.inbox;

import java.util.Comparator;

public interface BlueshiftInboxComparator extends Comparator<BlueshiftInboxMessage> {
    @Override
    int compare(BlueshiftInboxMessage message1, BlueshiftInboxMessage message2);
}
