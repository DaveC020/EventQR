package com.thedavelopers.eventqr.shared.utils;

import java.util.UUID;

public final class QrValueGenerator {

    private QrValueGenerator() {
    }

    public static String generate() {
        return "EVQR-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}

