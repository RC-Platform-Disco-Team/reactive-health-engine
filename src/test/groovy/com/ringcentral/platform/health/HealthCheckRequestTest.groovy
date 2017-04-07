package com.ringcentral.platform.health

import org.slf4j.Logger
import spock.lang.Specification

import java.time.Clock
import java.time.Duration

class HealthCheckRequestTest extends Specification {


    def "should set correct result and metadata for successful check"() {
        given:
        HealthCheckRequest req = new HealthCheckRequest(simplestHealthCheck, HealthCheckSignal.Type.PASSIVE, false, Duration.ofDays(20))

        when:
        def result = req.execute(Clock.systemUTC(), Mock(Logger))

        then:
        result.state == HealthStateEnum.OK
        result.message.empty
        result.executedAt != null
        result.duration.present
    }

    def "should set Warning state for the check which executes longer than specified"() {
        given:
        HealthCheckRequest req = new HealthCheckRequest(simplestHealthCheck, HealthCheckSignal.Type.PERIODIC, false, Duration.ofMillis(-1))

        when:
        def result = req.execute(Clock.systemUTC(), Mock(Logger))

        then:
        result.state == HealthStateEnum.Warning
        result.message.startsWith('Slow')
        result.executedAt != null
        result.duration.present
    }

    def simplestHealthCheck = new HealthCheckFunction() {
        @Override
        HealthCheckID getId() {
            return new HealthCheckID('unused')
        }

        @Override
        HealthCheckResult checkHealth() throws Exception {
            return ok()
        }
    }
}
