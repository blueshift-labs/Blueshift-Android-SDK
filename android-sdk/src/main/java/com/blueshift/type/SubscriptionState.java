package com.blueshift.type;

/**
 * @author Rahul Raveendran V P
 *         Created on 5/3/15 @ 3:03 PM
 *         https://github.com/rahulrvp
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
