package com.ringcentral.platform.rx;

import com.ringcentral.platform.health.HealthCheckResultWrapper;
import com.ringcentral.platform.health.HealthStateEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action;
import rx.functions.Action1;
import rx.functions.Func1;

import static com.ringcentral.platform.health.HealthCheckSignal.Type.PASSIVE;

/**
 * {@link rx.Observable.Operator} that distincts values with condition
 */
@Slf4j
@RequiredArgsConstructor
//public class DistinctResultsOperator<T, S> implements Observable.Operator<T, T> {
//
//    private final Func1<T, S> getStateFunction;
//
//    @Override
//    public Subscriber<? super T> call(Subscriber<? super T> child) {
//        return new Subscriber<T>(child) {
//
//            private S previousState;
//            private boolean hasPrevious;
//
//            @Override
//            public void onNext(T t) {
//                S newState = getStateFunction.call(t);
//
//                if (hasPrevious) {
//                    if (newState == previousState) {
//                        log.debug("Check result {} for healthcheck {} was filtered as unchanging", newState, t.getId());
//                        request(1);
//                    } else {
//                        child.onNext(t);
//                    }
//                } else {
//                    hasPrevious = true;
//                    child.onNext(t);
//                }
//                previousState = newState;
//            }
//
//            @Override
//            public void onError(Throwable e) {
//                child.onError(e);
//            }
//
//            @Override
//            public void onCompleted() {
//                child.onCompleted();
//            }
//
//        };
//    }
//}
@Deprecated
public class DistinctResultsOperator implements Observable.Operator<HealthCheckResultWrapper, HealthCheckResultWrapper> {

    @Override
    public Subscriber<? super HealthCheckResultWrapper> call(Subscriber<? super HealthCheckResultWrapper> child) {
        return new Subscriber<HealthCheckResultWrapper>(child) {

            private HealthStateEnum previousState;
            private boolean hasPrevious;

            @Override
            public void onNext(HealthCheckResultWrapper t) {
                HealthStateEnum newState = t.getState();

                if (hasPrevious) {
                    if (newState == previousState) {
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
