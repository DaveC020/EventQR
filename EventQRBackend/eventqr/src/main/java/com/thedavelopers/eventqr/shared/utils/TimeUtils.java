package com.thedavelopers.eventqr.shared.utils;

import java.time.Instant;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static Instant now() {
        return Instant.now();
    }
}

