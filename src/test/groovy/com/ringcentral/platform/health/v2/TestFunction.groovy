package com.ringcentral.platform.health.v2

import com.ringcentral.platform.health.HealthCheckFunction
import com.ringcentral.platform.health.HealthCheckID
import com.ringcentral.platform.health.HealthCheckResult

import java.time.Duration

import static com.ringcentral.platform.health.HealthStateEnum.OK

class TestFunction implements HealthCheckFunction {

    HealthCheckID name
    Duration duration
    Duration slowDuration
    Closure call

    TestFunction(String name, long duration, long slowDuration, Closure call) {
        this.name = new HealthCheckID(name)
        this.duration = Duration.ofMillis(duration)
        this.slowDuration = Duration.ofMillis(slowDuration)
        this.call = call
    }

    @Override
    HealthCheckID getId() {
        return name
    }

    @Override
    HealthCheckResult checkHealth() throws Exception {
        call(name, duration)
    }

    static checkHealth = { HealthCheckID name, Duration duration ->
        Thread.sleep(duration.toMillis())
        println(name.shortName + " in thread " + Thread.currentThread().getName())
        new HealthCheckResult(name, OK, "")
    }

    static checkHealthWithFail = { HealthCheckID name, Duration duration ->
        Thread.sleep(duration.toMillis())
        println(name.shortName + " in thread " + Thread.currentThread().getName())
        throw new RuntimeException("Fail during execution " + name.shortName)
    }


    static f(String name, long duration, long slowDuration, Closure call) {
        new TestFunction(name, duration, slowDuration, call)
    }

    static ok = { name -> f(name as String, 100, 1000, checkHealth) }
    static fail = { name -> f(name as String, 100, 1000, checkHealthWithFail) }
    static slow = { name -> f(name as String, 300, 100, checkHealth) }
    static timeout = { name -> f(name as String, 1100, 1600, checkHealth) }
    static timeoutFail = { name_ -> f(name_ as String, 0, 0, { name, duration ->
        try {
            println(name.shortName + " in thread " + Thread.currentThread().getName())
            Thread.sleep(100000)
        } catch (InterruptedException ignored) {
            println(name.shortName + " in thread " + Thread.currentThread().getName() + " interruption attempt")
            Thread.sleep(1000000)
        }

    }) }
}