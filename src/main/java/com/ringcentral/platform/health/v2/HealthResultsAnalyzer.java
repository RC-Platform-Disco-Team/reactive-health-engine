package com.ringcentral.platform.health.v2;

import com.ringcentral.platform.health.*;
import lombok.extern.slf4j.Slf4j;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.observers.SafeSubscriber;

public class HealthResultsAnalyzer extends SafeSubscriber<HealthCheckSignal> {

    private final Subscriber<? super HealthCheckSignal> actual;

    public HealthResultsAnalyzer(HealthState state) {
        super(new HealthResultsAnalyzerInt(state));
        actual = super.getActual();
    }

    @Override
    public void onNext(HealthCheckSignal args) {
        try {
            actual.onNext(args);
        } catch (Throwable e) {
            actual.onError(e);
        }
    }

    @Slf4j
    private static class HealthResultsAnalyzerInt extends Subscriber<HealthCheckSignal> {

        private final HealthState state;

        private HealthResultsAnalyzerInt(HealthState state) {
            this.state = state;
        }

        @Override
        public void onCompleted() {
            //do nothing
        }

        @Override
        public void onError(Throwable e) {
            log.warn(e.getMessage(), e);
        }

        @Override
        public void onNext(HealthCheckSignal signal) {
            if (signal == null) {
                log.error("Received null Health Check signal");
                return;
            }
//        try {
            switch (signal.getType()) {
                case PERIODIC:
                case FORCED:
                case PASSIVE:
                    processResult((HealthCheckResultWrapper) signal);
                    break;
                default:
                    log.warn("Received unsupported event type");
            }
//        } catch (Exception e) {
//            log.error("Enexpected exception during result analysis", e);
            //continue to work
//        }
        }

        private void processResult(HealthCheckResultWrapper result) {
            HealthCheckID id = result.getId();
            log.debug("Received {} health check result {} for {} on thread {}", result.getType(), result.getState(), id, Thread.currentThread().getName());
            state.updateState(result);
        }
    }
}