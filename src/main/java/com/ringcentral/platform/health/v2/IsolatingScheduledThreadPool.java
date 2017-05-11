package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.HealthCheckFunction;
import com.ringcentral.platform.health.HealthCheckID;
import lombok.extern.slf4j.Slf4j;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
class IsolatingScheduledThreadPool implements ScheduledThreadPool {

    private final Map<HealthCheckID, Scheduler> schedulers;
    private static final String defaultThreadNamePrefix = "health-";
    private static final Function<HealthCheckID, String> defaultThreadNamingFunction = id -> defaultThreadNamePrefix + id.getShortName();

    public IsolatingScheduledThreadPool(List<HealthCheckFunction> functions) {
        this(functions, defaultThreadNamingFunction);
    }

    public IsolatingScheduledThreadPool(List<HealthCheckFunction> functions, Function<HealthCheckID, String> threadNamingFunction) {
        schedulers = Collections.unmodifiableMap(functions == null ? Collections.emptyMap() :
                functions.stream().collect(
                        Collectors.toMap(HealthCheckFunction::getId,
                                t -> Schedulers.from(Executors.newSingleThreadExecutor(new DefaultThreadFactory(threadNamingFunction.apply(t.getId()))))
                        )
                )
        );
    }

    @Override
    public Optional<Scheduler> getScheduler(HealthCheckID id) {
        if (schedulers.containsKey(id)) {
            return Optional.of(schedulers.get(id));
        } else {
            log.warn("Unable to find scheduler for health check {}", id);
            return Optional.empty();
        }
    }

    private final class DefaultThreadFactory implements ThreadFactory {

        private final String threadName;

        DefaultThreadFactory(String threadName) {
            this.threadName = threadName;
        }

        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        }
    }
}
