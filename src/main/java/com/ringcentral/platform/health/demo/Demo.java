package com.ringcentral.platform.health.demo;

import com.ringcentral.platform.health.*;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class Demo {

    private static final TestFunction f1 = TestFunction.ok("f1");
    private static final TestFunction f2 = TestFunction.unstable("f2");

    private static List<HealthCheckFunction> functions = new ArrayList<HealthCheckFunction>() {{
        add(f1);
        add(f2);
    }};

    public static void main(String[] args) throws InterruptedException {
        HealthEngineConfigImpl engineConfig = HealthEngineConfigImpl.builder()
                .tickSignalPeriod(Duration.ofSeconds(1))
                .executionTimeout(Duration.ofMillis(1000))
                .build();
        HealthCheckConfig healthCfg = new HealthCheckConfig() {
            @Override
            public Duration getPeriod(HealthCheckID checkId) {
                if (checkId == f1.getId()) {
                    return Duration.ofSeconds(10);
                } else {
                    return Duration.ofSeconds(5);
                }
            }

            @Override
            public Duration getRetryPeriod(HealthCheckID checkId) {
                return Duration.ofSeconds(3);
            }

            @Override
            public Duration getSlowTimeout(HealthCheckID checkId) {
                return Duration.ofMillis(500);
            }

            @Override
            public boolean isDisabled(HealthCheckID checkId) {
                return false;
            }
        };

        final AtomicReference<HealthEngine> ref = new AtomicReference<>();
        UI window = createUI(functions, ref);
        Executors.newSingleThreadExecutor().submit(() -> {
                    ref.set(new ReactiveHealthEngine(healthCfg, engineConfig, Clock.systemUTC(), functions.toArray(new HealthCheckFunction[functions.size()])));
//                    ref.set(new ReactiveHealthEngineV2(healthCfg, engineConfig, Clock.systemUTC(), functions));
                }
        );
        while (true) {
            HealthEngine healthEngine = ref.get();
            if (healthEngine != null) {
                window.update(healthEngine.getGlobalState(), healthEngine.getLastChanged(), healthEngine.getAllResults());
            }
        }
    }

    private static UI createUI(List<HealthCheckFunction> functions, AtomicReference<HealthEngine> ref) {
        UI window = new UI(functions,
                () -> {
                    HealthEngine healthEngine = ref.get();
                    if (healthEngine != null) {
                        healthEngine.forceCheckSync();
                    }
                },
                () -> {
                    HealthEngine healthEngine = ref.get();
                    if (healthEngine != null) {
                        healthEngine.forceCheckAsync();
                    }
                },
                id -> {
                    HealthEngine healthEngine = ref.get();
                    if (healthEngine != null) {
                        healthEngine.sendPassiveCheckResult(new HealthCheckResult((HealthCheckID) id, HealthStateEnum.OK, ""));
                    }
                },
                id -> {
                    HealthEngine healthEngine = ref.get();
                    if (healthEngine != null) {
                        healthEngine.sendPassiveCheckResult(new HealthCheckResult((HealthCheckID) id, HealthStateEnum.Critical, ""));
                    }
                });
        window.pack();
        window.setVisible(true);
        return window;
    }
}
