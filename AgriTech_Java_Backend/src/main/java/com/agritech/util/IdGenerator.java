package com.agritech.util;

import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {
    private IdGenerator() {}

    public static String farmerId() {
        return "FRM" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 10_000_000_000L);
    }
}
