package com.turnout.common.exception;

public class TierLimitExceededException extends TurnoutException {

    public TierLimitExceededException(String feature, String currentTier) {
        super("Feature '" + feature + "' is not available on the " + currentTier + " plan", "TIER_LIMIT_EXCEEDED");
    }
}
