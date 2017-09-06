package com.ringcentral.platform.health;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class TimeFormatter {

    public static String printMmSs(Duration duration) {
        long secondsPart = duration.getSeconds() - TimeUnit.MINUTES.toSeconds(duration.toMinutes());

        if (duration.toMinutes() == 0) {
            return String.format("%ss", duration.getSeconds());
        } else if (secondsPart == 0) {
            return String.format("%sm", duration.toMinutes());
        } else {
            return String.format("%sm %ss", duration.toMinutes(), secondsPart);
        }
    }

    public static String printSsMs(Duration duration) {
        long millisPart = duration.toMillis() - TimeUnit.SECONDS.toMillis(duration.getSeconds());

        if (duration.getSeconds() == 0) {
            return String.format("%sms", duration.toMillis());
        } else if (millisPart == 0) {
            return String.format("%ss", duration.getSeconds());
        } else {
            return String.format("%ss %sms", duration.getSeconds(), millisPart);
        }
    }

    public static String format(Instant instant) {
        return DateTimeFormatter.ofPattern("dd-MM-uuuu HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault()).format(instant);
    }

}
