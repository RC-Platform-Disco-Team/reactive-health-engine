package com.ringcentral.health;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class DurationFormatter {
    private final Duration duration;

    public DurationFormatter(Duration duration) {
        this.duration = duration;
    }

    public String printMmSs() {
        long secondsPart = duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes());

        if (duration.toMinutes() == 0) {
            return String.format("%ss", duration.getSeconds());
        } else if (secondsPart == 0) {
            return String.format("%sm", duration.toMinutes());
        } else {
            return String.format("%sm %ss", duration.toMinutes(), secondsPart);
        }
    }

    public String printSsMs() {
        long millisPart = duration.toMillis() - TimeUnit.SECONDS.toMillis(duration.getSeconds());

        if (duration.getSeconds() == 0) {
            return String.format("%sms", duration.toMillis());
        } else if (millisPart == 0) {
            return String.format("%ss", duration.getSeconds());
        } else {
            return String.format("%ss %sms", duration.getSeconds(), millisPart);
        }
    }

}
