package com.transit.ticketing.contracts.tap;

public enum TapRejectionReason {
    MEDIA_BLOCKED,
    MEDIA_EXPIRED,
    INVALID_MEDIA,
    INSUFFICIENT_BALANCE,
    ACCOUNT_SUSPENDED,
    DUPLICATE_TAP,
    DEVICE_NOT_OPERATIONAL,
    INVALID_LOCATION,
    SYSTEM_ERROR
}
