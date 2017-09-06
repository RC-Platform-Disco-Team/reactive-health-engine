package com.ringcentral.platform.health.demo;

import com.ringcentral.platform.health.HealthCheckFunction;
import com.ringcentral.platform.health.HealthCheckID;
import com.ringcentral.platform.health.HealthCheckResult;
import lombok.Getter;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import static com.ringcentral.platform.health.HealthStateEnum.OK;

@Getter
public class TestFunction implements HealthCheckFunction {

    private HealthCheckID name;
    private Duration duration;
    private Duration slowDuration;
    private BiFunction<HealthCheckID, Duration, HealthCheckResult> call;

    public TestFunction(String name, long duration, long slowDuration, BiFunction<HealthCheckID, Duration, HealthCheckResult> call) {
        this.name = new HealthCheckID(name);
        this.duration = Duration.ofMillis(duration);
        this.slowDuration = Duration.ofMillis(slowDuration);
        this.call = call;
    }

    @Override
    public HealthCheckID getId() {
        return name;
    }

    @Override
    public HealthCheckResult checkHealth() throws Exception {
        return call.apply(name, duration);
    }

    public static HealthCheckResult checkHealth(HealthCheckID name, Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignored) {
        }
        System.out.println(name.getShortName() + " in thread " + Thread.currentThread().getName());
        return new HealthCheckResult(name, OK, "");
    }

    public static HealthCheckResult checkHealthWithFail(HealthCheckID name, Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException ignored) {
        }
        System.out.println(name.getShortName() + " in thread " + Thread.currentThread().getName());
        throw new RuntimeException("Fail during execution " + name.getShortName());
    }


    public static TestFunction f(String name, long duration, long slowDuration, BiFunction<HealthCheckID, Duration, HealthCheckResult> call) {
        return new TestFunction(name, duration, slowDuration, call);
    }

    public static TestFunction ok(String name) {
        return f(name, 100, 1000, TestFunction::checkHealth);
    }

    public static TestFunction fail(String name) {
        return f(name, 100, 1000, TestFunction::checkHealthWithFail);
    }

    public static TestFunction slow(String name) {
        return f(name, 300, 100, TestFunction::checkHealth);
    }

    public static TestFunction timeout(String name) {
        return f(name, 1100, 1600, TestFunction::checkHealth);
    }

    public static TestFunction timeoutFail(String name) {
        return f(name, 0, 0, (n, d) -> {
            try {
                System.out.println(n.getShortName() + " in thread " + Thread.currentThread().getName());
                Thread.sleep(100000);
            } catch (InterruptedException ignored) {
            }
            return new HealthCheckResult(n, OK, "");
        });
    }

    public static TestFunction unstable(String name) {
        AtomicInteger unstableCount = new AtomicInteger(0);
        return f(name, 100, 1000, (n, d) -> {
            if (unstableCount.incrementAndGet() % 5 != 0) {
                return checkHealth(n, d);
            } else {
                return checkHealthWithFail(n, d);
            }
        });
    }
}