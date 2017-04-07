package com.ringcentral.platform.health;

import org.slf4j.Logger;
import rx.Observable;
import rx.Subscriber;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PASSIVE;

class DistinctPassiveChecksOperator implements Observable.Operator<HealthCheckResultWrapper, HealthCheckResultWrapper> {

    private final Logger log;

    DistinctPassiveChecksOperator(Logger log) {
        this.log = log;
    }

    @Override
    public Subscriber<? super HealthCheckResultWrapper> call(Subscriber<? super HealthCheckResultWrapper> child) {
        return new Subscriber<HealthCheckResultWrapper>(child) {

            private HealthStateEnum previousState;
            private boolean hasPrevious;

            @Override
            public void onNext(HealthCheckResultWrapper t) {
                HealthStateEnum newState = t.getState();

                if (hasPrevious) {
                    if (t.getType() == PASSIVE && newState == previousState) {
                        log.debug("Check result {} for healthcheck {} was filtered as unchanging", newState, t.getId());
                        request(1);
                    } else {
                        child.onNext(t);
                    }
                } else {
                    hasPrevious = true;
                    child.onNext(t);
                }
                previousState = newState;
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onCompleted() {
                child.onCompleted();
            }

        };
    }
}
