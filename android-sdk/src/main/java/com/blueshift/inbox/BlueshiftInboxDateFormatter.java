package com.blueshift.inbox;

import androidx.annotation.NonNull;

import java.util.Date;

public interface BlueshiftInboxDateFormatter {
    @NonNull
    String format(Date date);
}
