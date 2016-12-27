package com.ringcentral.health

import spock.lang.Specification

import java.time.Duration

class DurationFormatterTest extends Specification {

    def "should display only present parts from minutes and seconds"() {
        expect:
        new DurationFormatter(source).printMmSs() == result

        where:
        source                 || result
        Duration.ofSeconds(61) || "1m 1s"
        Duration.ofSeconds(60) || "1m"
        Duration.ofSeconds(59) || "59s"
    }

    def "should display only present parts from seconds and millis"() {
        expect:
        new DurationFormatter(source).printSsMs() == result

        where:
        source                  || result
        Duration.ofSeconds(59)  || "59s"
        Duration.ofMillis(1001) || "1s 1ms"
        Duration.ofMillis(1)    || "1ms"
    }

}
