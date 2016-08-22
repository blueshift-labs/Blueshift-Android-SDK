package com.blueshift.type;

/**
 * Created by rahul on 5/3/15.
 */
public enum SubscriptionState {
    START,
    UPGRADE,
    DOWNGRADE;

    @Override
    public String toString() {
        switch (this) {
            case START:
                return "start";

            case UPGRADE:
                return "upgrade";

            case DOWNGRADE:
                return "downgrade";

            default:
                return super.toString();
        }
    }
}
